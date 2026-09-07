// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.call;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageInfo;
import com.hsc.api.service.ICallService;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.query.call.CallQuery;
import com.hsc.system.domain.query.call.CallRecordQuery;
import com.hsc.system.domain.vo.call.CallRecordVo;
import com.hsc.system.service.ICallRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 拨打接口
 * @author danmo
 * @date 2024年08月28日 17:38
 */
@Tag(name = "呼叫接口管理")
@RequestMapping("/v1/call")
@RestController
@Slf4j
public class CallController extends BaseController {

    @Autowired
    private ICallService iCallService;

    @Autowired
    private ICallRecordService callRecordService;

    /**
     * 创建呼叫
     * @return
     */
    @Operation(description = "创建呼叫", method = "POST")
    @PostMapping("/create")
    public ResResult makeCall(@RequestBody CallQuery query){
        Long callId = iCallService.makeCall(query);
        return success(callId);
    }

    /**
     * 呼叫详情
     * @return
     */
    @Operation(description = "呼叫详情", method = "POST")
    @PostMapping("/get/{callId}")
    public ResResult<CallRecordVo> getCallInfo(@PathVariable("callId") Long callId){
        CallRecordVo callRecord = iCallService.getCallInfo(callId);
        return success(callRecord);
    }

    @Operation(description = "呼叫列表(分页)", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<CallRecordVo>> getCallPageList(@RequestBody CallRecordQuery query){
        List<CallRecordVo> recordList = iCallService.getCallPageList(query);
        PageInfo<CallRecordVo> pageInfo = new PageInfo<>(recordList);
        return success(pageInfo);
    }

    /**
     * 录音播放：按 callId 查 file_path，流式返回 wav。
     * 不加 @PreAuthorize —— 走 SecurityConfig 默认 authenticated 兜底（登录即可播，不限按钮权限）。
     * 返回 void + 直写 HttpServletResponse（音频流非 ResResult，同 ExportController/testTts 范式）。
     */
    @Operation(summary = "录音播放(按callId流式返回wav)", method = "GET")
    @GetMapping("/record/play/{callId}")
    public void playRecord(@PathVariable("callId") String callId,
                           HttpServletResponse response) {
        CallRecord record = callRecordService.getByCallId(callId);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String audio = record.getFilePath();
        if (StrUtil.isBlank(audio) || !FileUtil.exist(audio)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = new File(audio);
        response.reset();
        response.setContentType("audio/wav");
        response.addHeader("Content-Disposition",
                "inline;filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8));
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
            log.error("录音播放失败 callId={} path={}", callId, audio, e);
            throw new CommonException("录音播放失败");
        }
    }
}
