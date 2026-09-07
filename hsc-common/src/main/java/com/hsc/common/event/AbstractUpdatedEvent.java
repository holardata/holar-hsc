package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 过程摘要更新事件：过程摘要 Quartz Job(每 2 分钟)生成摘要后发布，
 * 由 hsc-websocket 的 WsCallPushListener 监听推送 {@code ABSTRACT} 给对应坐席前端。
 *
 * <p>agentNumber 可空（Job 生成时若不便取），推送时由 WsCallPushService 按 callId 回查兜底。
 */
public class AbstractUpdatedEvent extends ApplicationEvent {

    private final String callId;
    private final String agentNumber;
    /** 过程摘要标题 */
    private final String title;
    /** 过程摘要内容 */
    private final String content;

    public AbstractUpdatedEvent(Object source, String callId, String agentNumber, String title, String content) {
        super(source);
        this.callId = callId;
        this.agentNumber = agentNumber;
        this.title = title;
        this.content = content;
    }

    public String getCallId() {
        return callId;
    }

    public String getAgentNumber() {
        return agentNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
