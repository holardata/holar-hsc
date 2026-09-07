package com.hsc.websocket.push;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.ISipAgentService;
import com.hsc.websocket.domain.WsEventEnum;
import com.hsc.websocket.domain.WsMsgPayload;
import com.hsc.websocket.service.IWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通话实时数据 WebSocket 推送实现。
 *
 * <p>定位链路(方案A)：callId → CallRecord.agentNumber → SipAgent.userId → sessionId，
 * 复用既有查询，不引入新 Redis 映射。
 *
 * <p>线程安全：{@code WebSocketSession.sendMessage} 非线程安全(同 session 并发 send 抛 IllegalStateException)，
 * 本类按 sessionId 串行化推送(对象锁 striped by sessionId)。多事件源(@Async 并发)下避免崩 session。
 *
 * <p>容错：任一跳为 null(CallRecord/SipAgent 未找到、坐席不在线)记 warn 并跳过，不抛异常(推送失败不应阻断业务)。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WsCallPushServiceImpl implements WsCallPushService {

    private final ICallRecordService callRecordService;
    private final ISipAgentService sipAgentService;
    private final IWebSocketService webSocketService;

    /** 按 sessionId 串行化推送的锁对象(striped locks)。 */
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    /** REALTIME_DIALOG 时间格式：与 DialogRecord.speakTime 的 @JsonFormat(GMT+8) 对齐,
     *  避免 WS(fastjson2 默认 UTC)与话单(Jackson GMT+8)出口差 8 小时。DateTimeFormatter 线程安全。 */
    private static final DateTimeFormatter DIALOG_TIME_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("GMT+8"));

    @Override
    public void pushRealtimeDialog(String callId, String channelType, String dialogTxt, String mode, Date createTime, String callerNumber) {
        String sessionId = resolveSessionIdByCallId(callId);
        if (sessionId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("callId", callId);
        data.put("channelType", channelType);
        data.put("callerNumber", callerNumber);
        data.put("dialogTxt", dialogTxt);
        data.put("mode", mode);
        // 格式化为北京壁钟时间串,与话单 speakTime(@JsonFormat GMT+8)出口一致
        data.put("createTime", createTime != null ? DIALOG_TIME_FMT.format(createTime.toInstant()) : null);
        safeSend(sessionId, WsEventEnum.REALTIME_DIALOG, data);
    }

    @Override
    public void pushCallStatus(String agentNumber, String callId, String statusType, String callerNumber) {
        String sessionId = resolveSessionIdByAgentNumber(agentNumber);
        if (sessionId == null) {
            // 客户侧 leg 的 open/close 无 agentNumber，按 callId 回查 CallRecord.agentNumber 兜底
            sessionId = resolveSessionIdByCallId(callId);
        }
        if (sessionId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("callId", callId);
        data.put("statusType", statusType);
        data.put("agentNumber", agentNumber);
        data.put("callerNumber", callerNumber);
        safeSend(sessionId, WsEventEnum.CALL_STATUS, data);
    }

    @Override
    public void pushAbstract(String agentNumber, String callId, String title, String content) {
        String sessionId = resolveSessionIdByAgentNumber(agentNumber);
        if (sessionId == null) {
            sessionId = resolveSessionIdByCallId(callId);
        }
        if (sessionId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("callId", callId);
        data.put("title", title);
        data.put("content", content);
        safeSend(sessionId, WsEventEnum.ABSTRACT, data);
    }

    @Override
    public void pushAgentRecommend(String callId, String recommendTxt, boolean isFinal) {
        String sessionId = resolveSessionIdByCallId(callId);
        if (sessionId == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("callId", callId);
        data.put("recommendTxt", recommendTxt);
        data.put("isFinal", isFinal);
        safeSend(sessionId, WsEventEnum.AGENT_RECOMMEND, data);
    }

    /** callId → CallRecord.agentNumber → SipAgent.userId → sessionId；任一跳缺失返回 null。 */
    private String resolveSessionIdByCallId(String callId) {
        if (StrUtil.isBlank(callId)) {
            return null;
        }
        CallRecord record = callRecordService.getByCallId(callId);
        if (record == null || StrUtil.isBlank(record.getAgentNumber())) {
            log.warn("ws 推送定位坐席失败：CallRecord/agentNumber 为空 callId={}", callId);
            return null;
        }
        return resolveSessionIdByAgentNumber(record.getAgentNumber());
    }

    /** agentNumber → SipAgent.userId → sessionId；任一跳缺失返回 null。 */
    private String resolveSessionIdByAgentNumber(String agentNumber) {
        if (StrUtil.isBlank(agentNumber)) {
            return null;
        }
        SipAgentVo agent = sipAgentService.getInfoByAgent(agentNumber);
        if (agent == null || agent.getUserId() == null) {
            log.warn("ws 推送定位坐席失败：SipAgent 未找到 agentNumber={}", agentNumber);
            return null;
        }
        String sessionId = webSocketService.getSessionId(String.valueOf(agent.getUserId()));
        if (sessionId == null) {
            log.warn("ws 推送跳过：坐席不在线 agentNumber={}, userId={}", agentNumber, agent.getUserId());
        }
        return sessionId;
    }

    /** 构造 WsMsgPayload 并按 sessionId 串行发送(WebSocketSession 非线程安全)。 */
    private void safeSend(String sessionId, WsEventEnum event, Object data) {
        String payload = WsMsgPayload.builder()
                .event(event)
                .timestamp(System.currentTimeMillis())
                .data(JSONObject.toJSONString(data))
                .build().toString();
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            try {
                webSocketService.sendMsg(sessionId, payload);
            } catch (Exception e) {
                log.error("ws 推送失败 sessionId={}, event={}", sessionId, event, e);
            }
        }
    }
}
