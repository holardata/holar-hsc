package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * 对话更新事件：ASR 文本落库后发布，由 hsc-websocket 监听推送 {@code REALTIME_DIALOG} 给坐席前端。
 *
 * <p>由 CallTranscriptListener 在落 dialog_record 后发布。
 */
public class DialogUpdatedEvent extends ApplicationEvent {

    /** 通话ID */
    private final String callId;
    /** 发言人类型 客户/坐席/AI智能坐席 */
    private final String channelType;
    /** 对话文本 */
    private final String dialogTxt;
    /** 显示模式 append(增量累加)|overwrite(终稿覆盖) */
    private final String mode;
    /** 消息时间 */
    private final Date createTime;
    /** 客户号码(CallRecord.callerNumber):每条转写都带,前端显示客户号不依赖 CALL_STATUS.open 时序 */
    private final String callerNumber;

    public DialogUpdatedEvent(Object source, String callId, String channelType, String dialogTxt, String mode, Date createTime, String callerNumber) {
        super(source);
        this.callId = callId;
        this.channelType = channelType;
        this.dialogTxt = dialogTxt;
        this.mode = mode;
        this.createTime = createTime;
        this.callerNumber = callerNumber;
    }

    public String getCallId() {
        return callId;
    }

    public String getChannelType() {
        return channelType;
    }

    public String getDialogTxt() {
        return dialogTxt;
    }

    public String getMode() {
        return mode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public String getCallerNumber() {
        return callerNumber;
    }
}
