package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * AI 推荐话术事件：客户 ASR 终稿触发 holargpt 流式返回的推荐话术 chunk，
 * 由 hsc-websocket 的 WsCallPushListener 监听推送 {@code AGENT_RECOMMEND} 给对应坐席前端。
 *
 * <p>由 CallTranscriptListener 在 holargpt SSE 每个 chunk 到达时发布（isFinal=false），
 * 流结束时发布收尾事件（isFinal=true）。
 */
public class AgentRecommendEvent extends ApplicationEvent {

    /** 通话ID（用于定位在线坐席 session） */
    private final String callId;
    /** 推荐话术增量文本（isFinal=true 时为空） */
    private final String recommendTxt;
    /** 是否流式结束（前端据此收尾） */
    private final boolean isFinal;

    public AgentRecommendEvent(Object source, String callId, String recommendTxt, boolean isFinal) {
        super(source);
        this.callId = callId;
        this.recommendTxt = recommendTxt;
        this.isFinal = isFinal;
    }

    public String getCallId() {
        return callId;
    }

    public String getRecommendTxt() {
        return recommendTxt;
    }

    public boolean isFinal() {
        return isFinal;
    }
}
