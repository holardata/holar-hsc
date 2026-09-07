package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 通话状态事件：session_open/close/transfer，由 hsc-websocket 监听推送 {@code CALL_STATUS} 给坐席前端。
 *
 * <p>由 CallTranscriptListener 在收到 ai-callbot 的 open/close/transfer 信号后发布。
 * 注：通话的权威开始/结束由 ESL CHANNEL_CREATE/HANGUP 驱动（见 1.11/1.12），
 * 本事件是 ai-callbot 旁路 leg 的状态信号，用于前端实时状态提示。
 */
public class CallStatusEvent extends ApplicationEvent {

    /** 通话ID */
    private final String callId;
    /** 状态类型 open|close|transfer */
    private final String statusType;
    /** 坐席工号(可空，客户侧 leg 无) */
    private final String agentNumber;
    /** 客户号码 */
    private final String callerNumber;

    public CallStatusEvent(Object source, String callId, String statusType, String agentNumber, String callerNumber) {
        super(source);
        this.callId = callId;
        this.statusType = statusType;
        this.agentNumber = agentNumber;
        this.callerNumber = callerNumber;
    }

    public String getCallId() {
        return callId;
    }

    public String getStatusType() {
        return statusType;
    }

    public String getAgentNumber() {
        return agentNumber;
    }

    public String getCallerNumber() {
        return callerNumber;
    }
}
