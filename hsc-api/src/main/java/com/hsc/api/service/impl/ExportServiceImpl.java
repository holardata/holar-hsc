package com.hsc.api.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.api.service.IExportService;
import com.hsc.api.util.DocxUtil;
import com.hsc.api.util.PdfUtil;
import com.hsc.api.util.ZipUtil;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.query.dialogue.DialogueExportQuery;
import com.hsc.system.service.IAbstractRecordService;
import com.hsc.system.service.ICallAiSummaryService;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.IDialogRecordService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 通话导出实现（平迁自 reminder_backend ExportController.exportHistoryRecord）。
 *
 * <p>适配 hsc 模型：rb HistoryRecord → hsc {@link CallRecord} + {@link CallAiSummary}；
 * rb sessionId → hsc callId；录音路径取 {@link CallRecord#getFilePath}（wav，不转码）。
 *
 * <p>文档生成格式判断(docx/pdf/txt)统一收口到 {@link #genDocument}，消除 rb 原文/AI改写/摘要三处重复。
 * 中文字体路径由配置 {@code hsc.export.ttf-path} 提供（默认容器路径，部署时可覆盖）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ExportServiceImpl implements IExportService {

    @Value("${hsc.export.ttf-path:/usr/share/fonts/chinese/msyh.ttf}")
    private String ttfPath;

    private static final String MSG_ASR = "ASR";

    private final ICallRecordService callRecordService;
    private final IDialogRecordService dialogRecordService;
    private final IAbstractRecordService abstractRecordService;
    private final ICallAiSummaryService callAiSummaryService;

    @Override
    public void exportCallRecord(DialogueExportQuery query, HttpServletResponse response) {
        if (StrUtil.isBlank(query.getCallId())) {
            throw new CommonException("callId不能为空");
        }
        CallRecord record = callRecordService.getByCallId(query.getCallId());
        if (record == null) {
            throw new CommonException("通话记录不存在");
        }
        String callId = record.getCallId();
        String dateStr = record.getCallStartTime() == null
                ? "unknown" : DateUtil.format(record.getCallStartTime(), "yyyy-MM-dd");

        String tmpDirStr = System.getProperty("java.io.tmpdir") + File.separator
                + IdUtil.getSnowflakeNextIdStr() + File.separator;
        FileUtil.mkdir(tmpDirStr);

        List<String> srcFiles = new ArrayList<>();
        List<String> logInfo = new ArrayList<>();
        logInfo.add("导出情况:");

        List<DialogRecord> dialogs = dialogRecordService.list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getMsgType, MSG_ASR)
                .orderByAsc(DialogRecord::getCreateTime));

        // 1. 原文文档
        if (CollUtil.isNotEmpty(dialogs) && StrUtil.isNotBlank(query.getDialogTxtFormat())) {
            String suffix = suffixOf(query.getDialogTxtFormat());
            List<String> lines = buildOriginLines(dialogs, query.getDialogTxtShowInfo());
            String outPath = tmpDirStr + "原文文档." + suffix;
            genDocument(lines, query.getDialogTxtFormat(), outPath);
            srcFiles.add(outPath);
            logInfo.add("1.原文 - 导出成功, 文件名=原文文档." + suffix);
        } else {
            logInfo.add(CollUtil.isEmpty(dialogs) ? "1.原文 - 没有任何数据" : "1.原文 - 用户选择不导出");
        }

        // 2. AI改写文档
        if (CollUtil.isNotEmpty(dialogs) && StrUtil.isNotBlank(query.getGaixieFormat())) {
            String suffix = suffixOf(query.getGaixieFormat());
            List<String> lines = buildGaixieLines(dialogs, query.getGaixieShowInfo(), query.getGaixieContent());
            String outPath = tmpDirStr + "AI改写文档." + suffix;
            genDocument(lines, query.getGaixieFormat(), outPath);
            srcFiles.add(outPath);
            logInfo.add("2.AI改写 - 导出成功, 文件名=AI改写文档." + suffix);
        } else {
            logInfo.add(CollUtil.isEmpty(dialogs) ? "2.AI改写 - 没有任何数据" : "2.AI改写 - 用户选择不导出");
        }

        // 3. 摘要文档
        if (StrUtil.isNotBlank(query.getSummaryFormat())) {
            String suffix = suffixOf(query.getSummaryFormat());
            CallAiSummary summary = callAiSummaryService.getOrCreateByCallId(callId);
            List<AbstractRecord> abstracts = abstractRecordService.list(new LambdaQueryWrapper<AbstractRecord>()
                    .eq(AbstractRecord::getCallId, callId).orderByAsc(AbstractRecord::getCreateTime));
            String content = buildSummaryText(summary, abstracts, query.getSummaryAll());
            String outPath = tmpDirStr + "摘要文档." + suffix;
            List<String> lines = new ArrayList<>();
            lines.add(content);
            genDocument(lines, query.getSummaryFormat(), outPath);
            srcFiles.add(outPath);
            logInfo.add("3.摘要 - 导出成功, 文件名=摘要文档." + suffix);
        } else {
            logInfo.add("3.摘要 - 用户选择不导出");
        }

        // 4. 录音文件
        if (StrUtil.isNotBlank(query.getAudioFileFormat())) {
            String audio = record.getFilePath();
            if (StrUtil.isNotBlank(audio) && FileUtil.exist(audio)) {
                srcFiles.add(audio);
                logInfo.add("4.录音文件 - 导出成功, 文件名=" + FileUtil.getName(audio));
            } else {
                logInfo.add("4.录音文件 - 音频文件缺失,导出失败.");
            }
        } else {
            logInfo.add("4.录音文件 - 用户选择不导出");
        }

        // 导出日志
        String logFilePath = tmpDirStr + "导出日志.log";
        FileUtil.writeUtf8Lines(logInfo, logFilePath);
        srcFiles.add(logFilePath);

        // 打包 zip 并写出响应
        String zipPath = tmpDirStr + dateStr + ".zip";
        ZipUtil.zipFiles(srcFiles, zipPath);
        writeResponse(response, zipPath);
    }

    /** 原文文档行：按 showInfo 附加 发言人/时间戳，再接文本。 */
    private List<String> buildOriginLines(List<DialogRecord> dialogs, String showInfo) {
        boolean showUser = showInfo != null && showInfo.contains("发言人");
        boolean showTime = showInfo != null && showInfo.contains("时间戳");
        List<String> lines = new ArrayList<>();
        for (DialogRecord d : dialogs) {
            lines.add(joinMeta(d, showUser, showTime));
            lines.add(nullToEmpty(d.getDialogTxt()));
        }
        return lines;
    }

    /** AI改写文档行：按 gaixieContent 决定 原文/改写/对照。 */
    private List<String> buildGaixieLines(List<DialogRecord> dialogs, String showInfo, String gaixieContent) {
        boolean showUser = showInfo != null && showInfo.contains("发言人");
        boolean showTime = showInfo != null && showInfo.contains("时间戳");
        boolean showYuanwen = "原文与改写对照".equals(gaixieContent);
        boolean showGaixie = "原文与改写对照".equals(gaixieContent) || "改写结果".equals(gaixieContent);
        boolean needPrefix = showYuanwen && showGaixie;
        List<String> lines = new ArrayList<>();
        if (!needPrefix) {
            if (showYuanwen) {
                lines.add("原文:");
            } else if (showGaixie) {
                lines.add("改写:");
            }
        }
        for (DialogRecord d : dialogs) {
            lines.add(joinMeta(d, showUser, showTime));
            if (showYuanwen) {
                lines.add(needPrefix ? "原文:" + nullToEmpty(d.getDialogTxt()) : nullToEmpty(d.getDialogTxt()));
            }
            if (showGaixie) {
                lines.add(needPrefix ? "改写:" + nullToEmpty(d.getGaixieTxt()) : nullToEmpty(d.getGaixieTxt()));
            }
        }
        return lines;
    }

    /** 摘要文档文本：按 summaryAll 选择关键词/全文总结/过程摘要/角色总结/要点/代办。 */
    private String buildSummaryText(CallAiSummary s, List<AbstractRecord> abstracts, String summaryAll) {
        String all = summaryAll == null ? "" : summaryAll;
        StringBuilder sb = new StringBuilder();
        if (all.contains("关键词")) {
            sb.append("关键词:\n").append(nullToEmpty(s.getKeywordAbstract())).append("\n\n");
        }
        if (all.contains("全文总结")) {
            sb.append("全文总结:\n").append(nullToEmpty(s.getSummary())).append("\n\n");
        }
        if (all.contains("过程摘要")) {
            sb.append("过程摘要:\n");
            for (AbstractRecord a : abstracts) {
                sb.append(" 标题:").append(nullToEmpty(a.getTitle())).append(":\n");
                sb.append(" 摘要:").append(nullToEmpty(a.getContent())).append("\n");
            }
        }
        if (all.contains("角色总结")) {
            sb.append("角色总结:\n").append(formatRoleSummary(s.getRoleSummary())).append("\n");
        }
        if (all.contains("要点提炼")) {
            sb.append("要点提炼:\n").append(nullToEmpty(s.getKeypointExtract())).append("\n\n");
        }
        if (all.contains("代办事项")) {
            sb.append("代办事项:\n").append(nullToEmpty(s.getTodoThings())).append("\n\n");
        }
        return sb.toString();
    }

    /** 角色总结 JSON 数组 → 文本；解析失败回退原始字符串。 */
    private String formatRoleSummary(String roleJson) {
        if (StrUtil.isBlank(roleJson)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray arr = JSON.parseArray(roleJson);
            if (arr == null || arr.isEmpty()) {
                return roleJson + "\n";
            }
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String roleName = obj.getString("roleName");
                String roleSummary = obj.getString("roleSummary");
                if (StrUtil.isBlank(roleName)) {
                    roleName = "发言人 " + (i + 1);
                }
                sb.append(" ").append(roleName).append(":\n");
                sb.append(" ").append(nullToEmpty(roleSummary)).append("\n");
            }
        } catch (Exception ex) {
            log.warn("解析角色总结JSON失败, 使用原始字符串: {}", ex.getMessage());
            sb.append(roleJson).append("\n");
        }
        return sb.toString();
    }

    /** 拼接每句的 发言人/时间戳 元信息。 */
    private String joinMeta(DialogRecord d, boolean showUser, boolean showTime) {
        List<String> meta = new ArrayList<>();
        if (showUser) {
            meta.add(StrUtil.isBlank(d.getChannelType()) ? "发言人" : d.getChannelType());
        }
        if (showTime && d.getCreateTime() != null) {
            meta.add(DateUtil.format(d.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
        }
        return String.join(" ", meta);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 按 format 选 docx/pdf/txt 生成文档（统一 rb 原文/AI改写/摘要三处重复判断）。 */
    private void genDocument(List<String> lines, String format, String outPath) {
        String f = format == null ? "" : format.toLowerCase();
        if (f.contains("docx")) {
            DocxUtil.genDocx(lines, outPath);
        } else if (f.contains("pdf")) {
            PdfUtil.genPdf(lines, ttfPath, outPath);
        } else {
            FileUtil.writeLines(lines, outPath, StandardCharsets.UTF_8);
        }
    }

    private String suffixOf(String format) {
        String f = format == null ? "" : format.toLowerCase();
        if (f.contains("docx")) {
            return "docx";
        }
        if (f.contains("pdf")) {
            return "pdf";
        }
        return "txt";
    }

    /** 流式写 zip 到 HTTP 响应（附件下载，8K buffer 避免大文件 OOM）。 */
    private void writeResponse(HttpServletResponse response, String zipPath) {
        File file = new File(zipPath);
        if (!file.exists()) {
            throw new CommonException("导出文件未生成: " + zipPath);
        }
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8));
        response.addHeader("Content-Length", String.valueOf(file.length()));
        try (OutputStream os = new BufferedOutputStream(response.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("写出导出 zip 失败 path={}", zipPath, e);
            throw new CommonException("导出失败: " + e.getMessage());
        }
    }
}
