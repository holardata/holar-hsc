package com.hsc.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * AI 转人工 originate 失败事件（忙/不可达），HangupComplete 发出、AiTransferService 监听重试下一个目标。
 */
@Getter
public class AiTransferRetryEvent extends ApplicationEvent {

    private final Long callId;
    private final String hangupCause;

    public AiTransferRetryEvent(Object source, Long callId, String hangupCause) {
        super(source);
        this.callId = callId;
        this.hangupCause = hangupCause;
    }
}
