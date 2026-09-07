package com.hsc.ivr.service;

import com.hsc.common.constant.FlowDataContext;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.service.IDialogRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * IVR 交互转写落库：把用户与 IVR 流程的交互过程写入 dialog_record（source=ivr），
 * 通话记录详情页转写区即可完整还原"系统播报了什么、用户按了什么键"。
 *
 * <p>记录语义（channel_type 沿用话单转写字典，前端按其渲染气泡左右侧）：
 * <ul>
 *   <li>播报（ivr_play）：channel=坐席（前端说话人显示"IVR语音导航"），text=播报文字原文
 *       （文本模式=渲染后内容；文件模式=语音文件的 speech_text/name）</li>
 *   <li>按键/事件（ivr_dtmf）：channel=客户，text=按键：x / 未按键 / 重试耗尽 / 未评价</li>
 *   <li>转接锚点（transfer）：复用 AI 转人工锚点格式，前端渲染"转接XX"分割线，
 *       后续人工/AI 段转写与 IVR 段在同一时间线衔接</li>
 * </ul>
 *
 * <p>所有方法均容错：落库失败只记日志不阻断流程（同 FlowSatisfactionHandler.saveRecord 风格）。
 * ivr_* 不进挂断摘要（buildAsrDialogText 只查 ASR/ai_answer），避免按键/播报文本污染 LLM 输入。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class IvrDialogLogger {

    public static final String SOURCE_IVR = "ivr";
    public static final String MSG_IVR_PLAY = "ivr_play";
    public static final String MSG_IVR_DTMF = "ivr_dtmf";
    private static final String MSG_TRANSFER = "transfer";

    private static final String CHANNEL_CUSTOMER = "客户";
    private static final String CHANNEL_AGENT = "坐席";

    private final IDialogRecordService dialogRecordService;

    /** 系统播报记录（每轮真正下发播放时调用，含重试轮的错误/未按键提示重播） */
    public void logPlay(FlowDataContext flowData, String text) {
        save(flowData, CHANNEL_AGENT, MSG_IVR_PLAY, text);
    }

    /** 用户按键记录：有键"按键：x"，空键（超时未按）"未按键" */
    public void logDtmf(FlowDataContext flowData, String digits) {
        save(flowData, CHANNEL_CUSTOMER, MSG_IVR_DTMF,
                digits == null || digits.isBlank() ? "未按键" : "按键：" + digits);
    }

    /** 客户侧事件记录：重试耗尽/未评价等 */
    public void logNote(FlowDataContext flowData, String text) {
        save(flowData, CHANNEL_CUSTOMER, MSG_IVR_DTMF, text);
    }

    /** 转接锚点（前端渲染分割线）：label=转接坐席/转接技能组/转接AI坐席/转接IVR流程… */
    public void logTransfer(FlowDataContext flowData, String label) {
        save(flowData, CHANNEL_AGENT, MSG_TRANSFER, label);
    }

    private void save(FlowDataContext flowData, String channelType, String msgType, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            DialogRecord dialog = new DialogRecord();
            dialog.setCallId(String.valueOf(flowData.getCallId()));
            dialog.setChannelType(channelType);
            dialog.setMsgType(msgType);
            dialog.setDialogTxt(text);
            dialog.setSource(SOURCE_IVR);
            dialog.setSpeakTime(new Date());
            dialog.setStatus(0);
            dialogRecordService.save(dialog);
        } catch (Exception e) {
            log.error("IVR交互转写落库失败 callId:{}, msgType:{}, text:{}, error:{}",
                    flowData.getCallId(), msgType, text, e.getMessage(), e);
        }
    }
}
