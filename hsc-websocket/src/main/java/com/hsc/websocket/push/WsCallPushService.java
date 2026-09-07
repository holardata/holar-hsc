package com.hsc.websocket.push;

import java.util.Date;

/**
 * 通话实时数据 WebSocket 推送服务（出站：后端 → 坐席前端）。
 *
 * <p>把通话实时转写 / 过程摘要 / 通话状态定向推给对应坐席。定位链路(方案A)：
 * {@code callId → CallRecord.agentNumber → SipAgent.userId → sessionId}，
 * 复用既有查询能力，不引入新 Redis 映射。
 *
 * <p>对应 WsEventEnum：{@code REALTIME_DIALOG} / {@code ABSTRACT} / {@code CALL_STATUS}。
 */
public interface WsCallPushService {

    /** 推送实时转写（按 callId 定位坐席）。callerNumber 随每条转写下发,前端显示客户号不依赖 CALL_STATUS 时序。 */
    void pushRealtimeDialog(String callId, String channelType, String dialogTxt, String mode, Date createTime, String callerNumber);

    /** 推送通话状态（优先用 agentNumber 定位，空则按 callId 回查兜底）。 */
    void pushCallStatus(String agentNumber, String callId, String statusType, String callerNumber);

    /** 推送过程摘要（优先用 agentNumber 定位，空则按 callId 回查兜底）。 */
    void pushAbstract(String agentNumber, String callId, String title, String content);

    /** 推送 AI 推荐话术（按 callId 定位坐席，AGENT_RECOMMEND 事件）。 */
    void pushAgentRecommend(String callId, String recommendTxt, boolean isFinal);
}
