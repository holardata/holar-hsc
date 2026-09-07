package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.DialogRecord;

import java.util.List;

/**
 * 通话逐句对话记录(dialog_record)表服务接口
 */
public interface IDialogRecordService extends IBaseService<DialogRecord> {

    /** 按 callId 查全部 ASR 句子(按说话时间升序)，用于挂断全文摘要拼文本。 */
    List<DialogRecord> listAsrByCallId(String callId);

    /** 有未处理(status=0)ASR 句子的 callId 列表(过程摘要定时扫描用)。 */
    List<String> listCallIdsWithPendingAsr();

    /** 该 callId 下未处理(status=0)的 ASR 句子(按说话时间升序)。 */
    List<DialogRecord> listPendingAsrByCallId(String callId);

    /** 标记该 callId 的未处理 ASR 为已处理(status=1)。 */
    void markProcessed(String callId);

    /**
     * 拼接该 callId 的 ASR 对话文本("发言人：文本"逐句换行)，作为 LLM 摘要输入。
     *
     * <p>挂断全文摘要(CallAiSummaryListener)与单字段重算(DialogueService.refreshByKey)共用，
     * 避免重复实现"取 ASR + 拼文本"逻辑。
     *
     * @return 拼接后的对话文本；无 ASR 记录返回空串
     */
    default String buildAsrDialogText(String callId) {
        // 纳入 ASR + ai_answer：摘要要含 AI 回复（仅 ASR 会让 LLM 只看到客户说的话、缺 AI 应答）
        // 但排除人工坐席推荐话术(source=recommend)：它是"建议坐席怎么说"，并非真实发生的对话，
        // 纳入会污染总结。总结只反映真实对话(客户/坐席 ASR + AI 接听的真实回复)。
        List<DialogRecord> dialogs = list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .in(DialogRecord::getMsgType, "ASR", "ai_answer")
                .ne(DialogRecord::getSource, "recommend")
                .orderByAsc(DialogRecord::getSpeakTime));
        if (dialogs == null || dialogs.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (DialogRecord d : dialogs) {
            text.append(d.getChannelType()).append("：").append(d.getDialogTxt()).append("\n");
        }
        return text.toString();
    }
}
