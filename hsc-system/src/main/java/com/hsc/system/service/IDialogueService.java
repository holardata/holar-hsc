package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.query.dialogue.AbstractRecordEditQuery;
import com.hsc.system.domain.query.dialogue.CallAiSummaryEditQuery;
import com.hsc.system.domain.query.dialogue.DialogueHistoryQuery;
import com.hsc.system.domain.vo.dialogue.DialogueHistoryVo;
import com.hsc.system.domain.vo.dialogue.FeedbackStatsVo;

import java.util.List;

/**
 * 通话对话业务编排服务（平迁自 reminder_backend HistoryRecordService + DialogueController 业务逻辑）。
 *
 * <p>适配 hsc 数据模型：
 * <ul>
 *   <li>rb HistoryRecord(通话+摘要合一) → hsc {@code CallRecord} + {@code CallAiSummary}</li>
 *   <li>rb sessionId → hsc callId</li>
 *   <li>rb HolarAiUtil.getAIAnswer(token,txt) → hsc {@link com.hsc.ai.service.ISummaryService#generate}，
 *       token 与 {@link com.hsc.ai.constant.PromptTitle} 常量一一对应</li>
 * </ul>
 *
 * <p>LLM 调用统一走 ISummaryService，业务代码不直连 LLM（遵循 CLAUDE.md 统一外部入口约束）。
 */
public interface IDialogueService {

    /** 通话历史分页（通话记录 + 摘要状态 + ASR/AI 句数统计）。 */
    PageInfo<DialogueHistoryVo> historyPage(DialogueHistoryQuery query);

    /** 按 callId 查单通话单详情（话单字段 + duration/routeName；无统计，供详情页"通话信息"卡片）。 */
    DialogueHistoryVo info(String callId);

    /** 按 callId 查客户侧 ASR 转写（msgType=ASR）。 */
    List<DialogRecord> userTxt(String callId);

    /** 按 callId 查 AI 回复（msgType=ai_answer）。 */
    List<DialogRecord> aiTxt(String callId);

    /** 按 callId 查过程摘要列表。 */
    List<AbstractRecord> abstractTxt(String callId);

    /** 按 callId 查挂断全文摘要（CallAiSummary 6 字段）；不存在返回 null（不创建空记录）。 */
    CallAiSummary summary(String callId);

    /** 按 callId 整通翻译（逐条 ASR 调 LLM 写 transTxt）。 */
    void translate(String callId);

    /** 按 callId 全局子串替换 dialogTxt。 */
    void replaceContent(String callId, String oldStr, String newStr);

    /** 按 dialogId 子串替换该条 dialogTxt。 */
    void replaceContentByDialogId(Long dialogId, String oldStr, String newStr);

    /** 按 dialogId 整体覆盖该条 dialogTxt。 */
    void replaceContentByDialogIdAll(Long dialogId, String newStr);

    /** 按 callId 整通 AI 改写（逐条 ASR 调 LLM 写 gaixieTxt）。 */
    void aigaixie(String callId);

    /** 按 callId 设置原文/改写显示开关（更新该 callId 所有 DialogRecord.gaixieWhether）。 */
    void showDiaglogGaixie(String callId, Integer show);

    /** 人工编辑摘要字段（CallAiSummary）。 */
    void editSummary(CallAiSummaryEditQuery query);

    /** 人工编辑过程摘要（AbstractRecord）。 */
    void editAbstract(AbstractRecordEditQuery query);

    /** 按 callId + key 重新生成单个摘要字段（summary/keyword/role/keypoint/todo/xmind）。 */
    void refreshByKey(String callId, String key);

    /** 摘要字段反馈（field=summary/keyword/role/keypoint/todo/xmind），写 CallAiSummary.xxxFeedback。 */
    void summaryFeedback(Long id, String field, Integer feedback);

    /** 过程摘要反馈，写 AbstractRecord.feedback/feedbackTxt。 */
    void abstractFeedback(Long id, Integer feedback, String feedbackTxt);

    /** 摘要反馈满意率统计（6 维度汇总 + 按天趋势）；beginDate/endDate 为 yyyy-MM-dd，可空=全量。 */
    FeedbackStatsVo feedbackStats(String beginDate, String endDate);

    /** 按 callId 批量删除通话及其对话/摘要（abstract_record/dialog_record/call_ai_summary/call_record）。 */
    boolean deleteByCallIds(List<String> callIds);
}
