package com.hsc.common.event;

import com.hsc.common.utils.VendorParamsCodec;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Redis 队列消息事件。
 *
 * <p>ai-callbot 推送到 {@code websocket:message:queue} 的消息，经 RedisMessageProcessor 解析参数头后发布，
 * 由业务层(hsc-system)的 {@code @EventListener} 监听处理（open/close/ASR/transfer 等业务）。
 * 平迁自 reminder_backend 在 RedisMessageProcessor 内直接处理的范式，改为事件驱动以解耦基础设施与业务。
 */
public class RedisQueueMessageEvent extends ApplicationEvent {

    /** 通话ID（= hsc CallRecord.callId） */
    private final String callId;
    /** 发言人角色 user/agent */
    private final String role;
    /** 坐席工号(= SipAgent.agentNumber)或 AIBOT */
    private final String agentId;
    /** 客户号码 */
    private final String userNumber;
    /** 来源 ai/bypass/asr_proxy */
    private final String source;
    /** 消息体：open/close/callee_uuid{path}/caller_uuid/ASR JSON/transfer 等 */
    private final String msgBody;

    public RedisQueueMessageEvent(Object source, Map<String, String> params, String msgBody) {
        super(source);
        this.callId = params.get(VendorParamsCodec.KEY_CALL_ID);
        this.role = params.get(VendorParamsCodec.KEY_ROLE);
        this.agentId = params.get(VendorParamsCodec.KEY_AGENT_ID);
        this.userNumber = params.get(VendorParamsCodec.KEY_USER_NUMBER);
        this.source = params.get(VendorParamsCodec.KEY_SOURCE);
        this.msgBody = msgBody;
    }

    public String getCallId() {
        return callId;
    }

    public String getRole() {
        return role;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public String getSource() {
        return source;
    }

    public String getMsgBody() {
        return msgBody;
    }
}
