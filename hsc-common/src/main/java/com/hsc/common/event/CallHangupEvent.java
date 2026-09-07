package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 通话挂断事件：通话最后一路挂断时由 ESL HANGUP_COMPLETE handler 发布，
 * 驱动 call-ai-summary capability 异步生成全文摘要（全文总结/关键词/角色/要点/代办/思维导图）。
 *
 * <p>只传 callId（不传 CallInfo 引用），因为发布后 HANGUP_COMPLETE 会清缓存。
 */
public class CallHangupEvent extends ApplicationEvent {

    private final String callId;

    public CallHangupEvent(Object source, String callId) {
        super(source);
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }
}
