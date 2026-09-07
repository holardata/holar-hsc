package com.hsc.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.hsc.ai.constant.PromptTitle;
import com.hsc.ai.service.ISummaryService;
import com.hsc.common.enums.FsHangupCauseEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.CallRoute;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.query.dialogue.AbstractRecordEditQuery;
import com.hsc.system.domain.query.dialogue.CallAiSummaryEditQuery;
import com.hsc.system.domain.query.dialogue.DialogueHistoryQuery;
import com.hsc.system.domain.vo.dialogue.DialogueHistoryVo;
import com.hsc.system.domain.vo.dialogue.FeedbackStatsVo;
import com.hsc.system.mapper.AbstractRecordMapper;
import com.hsc.system.mapper.CallAiSummaryMapper;
import com.hsc.system.service.IAbstractRecordService;
import com.hsc.system.service.ICallAiSummaryService;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.ICallRouteService;
import com.hsc.system.service.IDialogRecordService;
import com.hsc.system.service.IDialogueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 通话对话业务编排实现（平迁自 reminder_backend HistoryRecordServiceImpl + DialogueController 业务逻辑）。
 *
 * <p>适配 hsc 数据模型：
 * <ul>
 *   <li>rb HistoryRecord(通话+摘要合一) → hsc {@link CallRecord} + {@link CallAiSummary}</li>
 *   <li>rb sessionId → hsc callId</li>
 *   <li>rb HolarAiUtil.getAIAnswer(token,txt) → {@link ISummaryService#generate}，
 *       token 与 {@link PromptTitle} 常量一一对应（统一 LLM 出口，不直连 SDK）</li>
 * </ul>
 *
 * <p>单字段重算(refreshByKey)复用 {@link IDialogRecordService#buildAsrDialogText}，
 * 与挂断全文摘要(CallAiSummaryListener)共用同一套"取 ASR + 拼文本 + 调 ISummaryService"逻辑，避免重复实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DialogueServiceImpl implements IDialogueService {

    private static final String MSG_ASR = "ASR";
    private static final String MSG_AI_ANSWER = "ai_answer";
    private static final String MSG_TRANSFER = "transfer";
    /** IVR 交互转写：按键/事件记录（与 hsc-ivr IvrDialogLogger 落库值对齐） */
    private static final String MSG_IVR_DTMF = "ivr_dtmf";
    /** IVR 交互转写：系统播报记录（与 hsc-ivr IvrDialogLogger 落库值对齐） */
    private static final String MSG_IVR_PLAY = "ivr_play";

    /** 满意率统计的反馈维度：call_ai_summary 5 个 + 过程摘要 1 个（xmind 无前端反馈入口，不统计） */
    private static final List<String> SUMMARY_FEEDBACK_DIMS = List.of("summary", "keyword", "role", "keypoint", "todo");
    private static final List<String> ABSTRACT_FEEDBACK_DIMS = List.of("abstract");

    private final ICallRecordService callRecordService;
    private final ICallRouteService callRouteService;
    private final IDialogRecordService dialogRecordService;
    private final IAbstractRecordService abstractRecordService;
    private final ICallAiSummaryService callAiSummaryService;
    private final ISummaryService summaryService;
    private final CallAiSummaryMapper callAiSummaryMapper;
    private final AbstractRecordMapper abstractRecordMapper;

    // ============================== 查询 ==============================

    @Override
    public PageInfo<DialogueHistoryVo> historyPage(DialogueHistoryQuery query) {
        PageHelper.startPage(query.getPageIndex(), query.getPageSize());
        List<CallRecord> records = callRecordService.list(new LambdaQueryWrapper<CallRecord>()
                .eq(StrUtil.isNotBlank(query.getCallId()), CallRecord::getCallId, query.getCallId())
                .like(StrUtil.isNotBlank(query.getCallerNumber()), CallRecord::getCallerNumber, query.getCallerNumber())
                .like(StrUtil.isNotBlank(query.getCalleeNumber()), CallRecord::getCalleeNumber, query.getCalleeNumber())
                .eq(StrUtil.isNotBlank(query.getAgentNumber()), CallRecord::getAgentNumber, query.getAgentNumber())
                .like(StrUtil.isNotBlank(query.getAgentName()), CallRecord::getAgentName, query.getAgentName())
                .eq(query.getDirection() != null, CallRecord::getDirection, query.getDirection())
                .ge(query.getBeginTime() != null, CallRecord::getCallStartTime, query.getBeginTime())
                .le(query.getEndTime() != null, CallRecord::getCallStartTime, query.getEndTime())
                .orderByDesc(CallRecord::getCallStartTime));
        PageInfo<CallRecord> srcPage = new PageInfo<>(records);
        List<DialogueHistoryVo> voList = records.stream().map(this::toHistoryVo).collect(Collectors.toList());
        PageInfo<DialogueHistoryVo> result = new PageInfo<>();
        BeanUtil.copyProperties(srcPage, result, "list");
        result.setList(voList);
        return result;
    }

    @Override
    public DialogueHistoryVo info(String callId) {
        CallRecord record = callRecordService.getByCallId(callId);
        if (record == null) {
            throw new CommonException("话单不存在");
        }
        return toBaseVo(record);
    }

    private DialogueHistoryVo toHistoryVo(CallRecord record) {
        DialogueHistoryVo vo = toBaseVo(record);
        String callId = record.getCallId();
        if (StrUtil.isNotBlank(callId)) {
            vo.setUserTxtNum(dialogRecordService.count(new LambdaQueryWrapper<DialogRecord>()
                    .eq(DialogRecord::getCallId, callId).eq(DialogRecord::getMsgType, MSG_ASR)));
            vo.setAiTxtNum(dialogRecordService.count(new LambdaQueryWrapper<DialogRecord>()
                    .eq(DialogRecord::getCallId, callId)
                    .eq(DialogRecord::getMsgType, MSG_AI_ANSWER)
                    .ne(DialogRecord::getDialogTxt, "")));
            CallAiSummary summary = callAiSummaryService.getOrCreateByCallId(callId);
            if (summary.getId() != null) {
                vo.setSummaryStatus(summary.getSummaryStatus());
                vo.setKeywordStatus(summary.getKeywordStatus());
                vo.setRoleStatus(summary.getRoleStatus());
                vo.setKeypointStatus(summary.getKeypointStatus());
                vo.setTodoStatus(summary.getTodoStatus());
                vo.setXmindStatus(summary.getXmindStatus());
            }
        }
        return vo;
    }

    /**
     * CallRecord → DialogueHistoryVo 基础字段（话单 + duration + routeName + 挂断/接通衍生字段）。
     * historyPage（含统计）与 info（详情页通话信息卡片）共用。
     * 路由目标/日程名的解析在 hsc-api DialogueController（flow_info 实体在 hsc-ivr，system 模块不可依赖）。
     */
    private DialogueHistoryVo toBaseVo(CallRecord record) {
        DialogueHistoryVo vo = new DialogueHistoryVo();
        BeanUtil.copyProperties(record, vo);
        if (record.getCallStartTime() != null && record.getCallEndTime() != null) {
            vo.setDuration((record.getCallEndTime().getTime() - record.getCallStartTime().getTime()) / 1000);
        }
        // 接通耗时：呼入开始到真人/AI 接上（首次 bridge）；未接通（无 bridge）为 null。
        // 不用 ringingTime(=被叫开始振铃,摘机时长没算进)、answerTime(语义随链路漂移)
        if (record.getCallStartTime() != null && record.getBridgeTime() != null) {
            vo.setConnectSeconds((record.getBridgeTime().getTime() - record.getCallStartTime().getTime()) / 1000);
        }
        // 挂断原因文案（码表 FsHangupCauseEnum）
        if (record.getHangupCauseCode() != null) {
            FsHangupCauseEnum cause = FsHangupCauseEnum.getByCode(record.getHangupCauseCode());
            if (cause != null) {
                vo.setHangupCauseName(cause.getDesc());
            }
        }
        if (record.getRouteId() != null) {
            CallRoute route = callRouteService.getById(record.getRouteId());
            if (route != null) {
                vo.setRouteName(route.getName());
            }
        }
        return vo;
    }

    @Override
    public List<DialogRecord> userTxt(String callId) {
        // 纳入 transfer 锚点（话单详情时间线渲染"转接XX"分割线）与 IVR 按键/事件记录（客户侧气泡）
        return dialogRecordService.list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .in(DialogRecord::getMsgType, MSG_ASR, MSG_TRANSFER, MSG_IVR_DTMF)
                .orderByAsc(DialogRecord::getCreateTime));
    }

    @Override
    public List<DialogRecord> aiTxt(String callId) {
        // 纳入 IVR 系统播报记录（channel=坐席，前端说话人显示"语音导航"）
        return dialogRecordService.list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .in(DialogRecord::getMsgType, MSG_AI_ANSWER, MSG_IVR_PLAY)
                .ne(DialogRecord::getDialogTxt, "")
                .orderByAsc(DialogRecord::getCreateTime));
    }

    @Override
    public List<AbstractRecord> abstractTxt(String callId) {
        return abstractRecordService.list(new LambdaQueryWrapper<AbstractRecord>()
                .eq(AbstractRecord::getCallId, callId)
                .orderByAsc(AbstractRecord::getCreateTime));
    }

    @Override
    public CallAiSummary summary(String callId) {
        if (StrUtil.isBlank(callId)) {
            return null;
        }
        // 纯查询，不创建（getOrCreateByCallId 会建空记录，仅写入/编辑场景用）
        return callAiSummaryService.getOne(new LambdaQueryWrapper<CallAiSummary>()
                .eq(CallAiSummary::getCallId, callId)
                .last("LIMIT 1"));
    }

    // ============================== 翻译 / 改写 / 替换 ==============================

    @Override
    public void translate(String callId) {
        for (DialogRecord d : listAsrByCallId(callId)) {
            if (StrUtil.isBlank(d.getDialogTxt())) {
                continue;
            }
            String trans = summaryService.generate(callId, PromptTitle.TRANS_ZH_EN, d.getDialogTxt());
            DialogRecord up = new DialogRecord();
            up.setId(d.getId());
            up.setTransTxt(trans);
            dialogRecordService.updateById(up);
        }
    }

    @Override
    public void aigaixie(String callId) {
        for (DialogRecord d : listAsrByCallId(callId)) {
            if (StrUtil.isBlank(d.getDialogTxt())) {
                continue;
            }
            String gaixie = summaryService.generate(callId, PromptTitle.AI_GAIXIE, d.getDialogTxt());
            DialogRecord up = new DialogRecord();
            up.setId(d.getId());
            // 前端要求改写为空时显示"无"（平迁 rb 行为）
            up.setGaixieTxt(StrUtil.isBlank(gaixie) ? "无" : gaixie);
            // 改写生成后默认"原文+改写"显示
            up.setGaixieWhether(2);
            dialogRecordService.updateById(up);
        }
    }

    @Override
    public void showDiaglogGaixie(String callId, Integer show) {
        if (StrUtil.isBlank(callId)) {
            return;
        }
        if (show == null || (show != 0 && show != 1 && show != 2)) {
            throw new CommonException("显示模式取值非法(应为0=不显示/1=仅改写/2=原文+改写)");
        }
        DialogRecord up = new DialogRecord();
        up.setGaixieWhether(show);
        dialogRecordService.update(up, new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId));
    }

    @Override
    public void replaceContent(String callId, String oldStr, String newStr) {
        if (StrUtil.isBlank(oldStr)) {
            throw new CommonException("被替换内容不能为空");
        }
        // 只改 ASR 条（与前端转写区 matches 口径一致；AI 回复/转接不改）
        List<DialogRecord> dialogs = dialogRecordService.list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getMsgType, MSG_ASR)
                .like(DialogRecord::getDialogTxt, oldStr)
                .orderByAsc(DialogRecord::getCreateTime));
        for (DialogRecord d : dialogs) {
            replaceInDialog(d, oldStr, newStr);
        }
    }

    @Override
    public void replaceContentByDialogId(Long dialogId, String oldStr, String newStr) {
        DialogRecord d = dialogRecordService.getById(dialogId);
        if (d == null) {
            throw new CommonException("dialogId在数据库中不存在！");
        }
        if (StrUtil.isBlank(oldStr)) {
            throw new CommonException("被替换内容不能为空");
        }
        replaceInDialog(d, oldStr, newStr);
    }

    @Override
    public void replaceContentByDialogIdAll(Long dialogId, String newStr) {
        DialogRecord d = dialogRecordService.getById(dialogId);
        if (d == null) {
            throw new CommonException("dialogId在数据库中不存在！");
        }
        DialogRecord up = new DialogRecord();
        up.setId(dialogId);
        up.setDialogTxt(newStr);
        // 原文整句覆盖后，若该条已有译文则重译（中译英）
        if (StrUtil.isNotBlank(newStr) && StrUtil.isNotBlank(d.getTransTxt())) {
            up.setTransTxt(summaryService.generate(d.getCallId(), PromptTitle.TRANS_ZH_EN, newStr));
        }
        dialogRecordService.updateById(up);
    }

    /** 单条 dialogTxt 子串替换，同步替换该条 gaixieTxt（若存在）；原文变更后已有译文重译，避免残留。 */
    private void replaceInDialog(DialogRecord d, String oldStr, String newStr) {
        DialogRecord up = new DialogRecord();
        up.setId(d.getId());
        String newDialogTxt = null;
        if (StrUtil.isNotBlank(d.getDialogTxt())) {
            newDialogTxt = d.getDialogTxt().replace(oldStr, newStr);
            up.setDialogTxt(newDialogTxt);
        }
        if (StrUtil.isNotBlank(d.getGaixieTxt())) {
            up.setGaixieTxt(d.getGaixieTxt().replace(oldStr, newStr));
        }
        // 原文变更后，若该条已有译文则重译（中译英），避免译文残留旧原文
        if (StrUtil.isNotBlank(newDialogTxt) && StrUtil.isNotBlank(d.getTransTxt())) {
            up.setTransTxt(summaryService.generate(d.getCallId(), PromptTitle.TRANS_ZH_EN, newDialogTxt));
        }
        dialogRecordService.updateById(up);
    }

    // ============================== 编辑 / 重算 ==============================

    @Override
    public void editSummary(CallAiSummaryEditQuery query) {
        CallAiSummary s = locateSummary(query.getId(), query.getCallId());
        if (s == null || s.getId() == null) {
            throw new CommonException("摘要记录不存在");
        }
        CallAiSummary up = new CallAiSummary();
        up.setId(s.getId());
        boolean edited = false;
        if (StrUtil.isNotBlank(query.getSummary())) {
            up.setSummary(query.getSummary());
            edited = true;
        }
        if (StrUtil.isNotBlank(query.getKeywordAbstract())) {
            up.setKeywordAbstract(query.getKeywordAbstract().replace("，", ","));
            edited = true;
        }
        if (StrUtil.isNotBlank(query.getRoleSummary())) {
            up.setRoleSummary(query.getRoleSummary());
            edited = true;
        }
        if (StrUtil.isNotBlank(query.getKeypointExtract())) {
            up.setKeypointExtract(query.getKeypointExtract());
            edited = true;
        }
        if (StrUtil.isNotBlank(query.getTodoThings())) {
            up.setTodoThings(query.getTodoThings());
            edited = true;
        }
        if (StrUtil.isNotBlank(query.getMarkdownJson())) {
            up.setMarkdownJson(query.getMarkdownJson());
            edited = true;
        }
        if (edited) {
            callAiSummaryService.updateById(up);
        }
    }

    @Override
    public void editAbstract(AbstractRecordEditQuery query) {
        if (query.getId() == null) {
            throw new CommonException("修改对象的id不能为空！");
        }
        if (abstractRecordService.getById(query.getId()) == null) {
            throw new CommonException("修改对象的id在数据库中不存在！");
        }
        AbstractRecord up = new AbstractRecord();
        up.setId(query.getId());
        if (StrUtil.isNotBlank(query.getDialogTxt())) {
            up.setDialogTxt(query.getDialogTxt());
        }
        if (StrUtil.isNotBlank(query.getTitle())) {
            up.setTitle(query.getTitle());
        }
        if (StrUtil.isNotBlank(query.getContent())) {
            up.setContent(query.getContent());
        }
        abstractRecordService.updateById(up);
    }

    @Override
    public void refreshByKey(String callId, String key) {
        if (StrUtil.isBlank(callId) || StrUtil.isBlank(key)) {
            throw new CommonException("callId 与 key 不能为空");
        }
        String dialogText = dialogRecordService.buildAsrDialogText(callId);
        if (StrUtil.isBlank(dialogText)) {
            log.info("refreshByKey 跳过：无 ASR 记录 callId={}", callId);
            return;
        }
        CallAiSummary s = callAiSummaryService.getOrCreateByCallId(callId);
        if (s.getId() == null) {
            callAiSummaryService.save(s);
        }
        CallAiSummary up = new CallAiSummary();
        up.setId(s.getId());
        switch (key.toLowerCase()) {
            case "summary":
                up.setSummary(safe(summaryService.generate(callId, PromptTitle.OVERALL_SUMMARY, dialogText)));
                up.setSummaryStatus(2);
                break;
            case "keyword":
            case "keywordabstract":
                up.setKeywordAbstract(safe(summaryService.generate(callId, PromptTitle.KEYWORD_ABSTRACT, dialogText)));
                up.setKeywordStatus(2);
                break;
            case "role":
            case "rolesummary":
                // 走 generateRoleSummary(与挂断摘要同一规整入口)：LLM 偶发缺 [ ] 需落库前补齐
                up.setRoleSummary(summaryService.generateRoleSummary(callId, dialogText));
                up.setRoleStatus(2);
                break;
            case "keypoint":
            case "keypointextract":
                up.setKeypointExtract(safe(summaryService.generate(callId, PromptTitle.KEYPOINT_EXTRACT, dialogText)));
                up.setKeypointStatus(2);
                break;
            case "todo":
            case "todothings":
                up.setTodoThings(safe(summaryService.generate(callId, PromptTitle.TODO_THINGS, dialogText)));
                up.setTodoStatus(2);
                break;
            case "xmind":
            case "xmindjson":
            case "markdownjson":
                up.setMarkdownJson(summaryService.generateXmindJson(callId, dialogText));
                up.setXmindStatus(2);
                break;
            default:
                throw new CommonException("不支持的字段: " + key);
        }
        callAiSummaryService.updateById(up);
    }

    // ============================== 反馈 / 删除 ==============================

    @Override
    public void summaryFeedback(Long id, String field, Integer feedback) {
        if (id == null) {
            throw new CommonException("id不能为空");
        }
        if (callAiSummaryService.getById(id) == null) {
            throw new CommonException("记录不存在");
        }
        CallAiSummary up = new CallAiSummary();
        up.setId(id);
        switch (field == null ? "" : field.toLowerCase()) {
            case "summary":
                up.setSummaryFeedback(feedback);
                break;
            case "keyword":
                up.setKeywordFeedback(feedback);
                break;
            case "role":
                up.setRoleFeedback(feedback);
                break;
            case "keypoint":
                up.setKeypointFeedback(feedback);
                break;
            case "todo":
                up.setTodoFeedback(feedback);
                break;
            case "xmind":
                up.setXmindFeedback(feedback);
                break;
            default:
                throw new CommonException("不支持的字段: " + field);
        }
        callAiSummaryService.updateById(up);
    }

    @Override
    public void abstractFeedback(Long id, Integer feedback, String feedbackTxt) {
        if (id == null) {
            throw new CommonException("id不能为空");
        }
        if (abstractRecordService.getById(id) == null) {
            throw new CommonException("记录不存在");
        }
        AbstractRecord up = new AbstractRecord();
        up.setId(id);
        up.setFeedback(feedback);
        up.setFeedbackTxt(feedbackTxt);
        abstractRecordService.updateById(up);
    }

    /** 摘要反馈满意率统计：两表按天聚合合并，汇总 = 各天累加，无数据维度按固定顺序占位 */
    @Override
    public FeedbackStatsVo feedbackStats(String beginDate, String endDate) {
        // date -> dim -> [good, bad]（TreeMap 保日期升序；两表同日行合并）
        Map<String, Map<String, long[]>> byDate = new TreeMap<>();
        accumulateTrend(byDate, SUMMARY_FEEDBACK_DIMS, callAiSummaryMapper.selectFeedbackTrend(beginDate, endDate));
        accumulateTrend(byDate, ABSTRACT_FEEDBACK_DIMS, abstractRecordMapper.selectFeedbackTrend(beginDate, endDate));

        // 汇总 = 各天累加；趋势 = 逐天转 TrendPoint
        Map<String, long[]> totals = new LinkedHashMap<>();
        List<FeedbackStatsVo.TrendPoint> trend = new ArrayList<>();
        byDate.forEach((date, dims) -> {
            Map<String, FeedbackStatsVo.DimTotal> dayDims = new LinkedHashMap<>();
            dims.forEach((dim, cnt) -> {
                dayDims.put(dim, toDimTotal(dim, cnt));
                long[] sum = totals.computeIfAbsent(dim, k -> new long[2]);
                sum[0] += cnt[0];
                sum[1] += cnt[1];
            });
            FeedbackStatsVo.TrendPoint point = new FeedbackStatsVo.TrendPoint();
            point.setDate(date);
            point.setDims(dayDims);
            trend.add(point);
        });

        // 无数据维度也按固定顺序占位，前端坐标/图例稳定
        List<FeedbackStatsVo.DimTotal> items = new ArrayList<>();
        List<String> allDims = new ArrayList<>(SUMMARY_FEEDBACK_DIMS);
        allDims.addAll(ABSTRACT_FEEDBACK_DIMS);
        for (String dim : allDims) {
            long[] sum = totals.get(dim);
            items.add(toDimTotal(dim, sum != null ? sum : new long[2]));
        }

        FeedbackStatsVo vo = new FeedbackStatsVo();
        vo.setItems(items);
        vo.setTrend(trend);
        return vo;
    }

    /** 把一行按天聚合结果累进 byDate（列名 = dim + "Good"/"Bad"） */
    private void accumulateTrend(Map<String, Map<String, long[]>> byDate, List<String> dims, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Map<String, long[]> day = byDate.computeIfAbsent(String.valueOf(row.get("statDate")), k -> new LinkedHashMap<>());
            for (String dim : dims) {
                long[] cnt = day.computeIfAbsent(dim, k -> new long[2]);
                cnt[0] += toLong(row.get(dim + "Good"));
                cnt[1] += toLong(row.get(dim + "Bad"));
            }
        }
    }

    private FeedbackStatsVo.DimTotal toDimTotal(String dim, long[] cnt) {
        FeedbackStatsVo.DimTotal total = new FeedbackStatsVo.DimTotal();
        total.setKey(dim);
        total.setGood(cnt[0]);
        total.setBad(cnt[1]);
        return total;
    }

    /** SQL SUM 聚合列在 Map 里可能是 BigDecimal/Long，统一转 long */
    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByCallIds(List<String> callIds) {
        if (CollUtil.isEmpty(callIds)) {
            return false;
        }
        for (String callId : callIds) {
            if (StrUtil.isBlank(callId)) {
                continue;
            }
            abstractRecordService.remove(new LambdaQueryWrapper<AbstractRecord>().eq(AbstractRecord::getCallId, callId));
            dialogRecordService.remove(new LambdaQueryWrapper<DialogRecord>().eq(DialogRecord::getCallId, callId));
            callAiSummaryService.remove(new LambdaQueryWrapper<CallAiSummary>().eq(CallAiSummary::getCallId, callId));
            CallRecord cr = callRecordService.getByCallId(callId);
            if (cr != null && cr.getId() != null) {
                callRecordService.removeById(cr.getId());
            }
        }
        return true;
    }

    // ============================== 辅助 ==============================

    private List<DialogRecord> listAsrByCallId(String callId) {
        return dialogRecordService.list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getMsgType, MSG_ASR)
                .orderByAsc(DialogRecord::getCreateTime));
    }

    private CallAiSummary locateSummary(Long id, String callId) {
        if (id != null) {
            return callAiSummaryService.getById(id);
        }
        if (StrUtil.isNotBlank(callId)) {
            return callAiSummaryService.getOrCreateByCallId(callId);
        }
        return null;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
