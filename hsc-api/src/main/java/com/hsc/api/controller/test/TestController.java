package com.hsc.api.controller.test;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.hsc.common.base.BaseController;
import com.hsc.common.utils.VendorParamsCodec;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.ISipAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * ⚠️ 临时测试接口：模拟 ai-callbot 旁路 ASR 推 Redis 队列，驱动 hsc → WS → 前端工作台链路。
 * <p>测完删除（含 SecurityConfig 白名单 /test/**）。
 *
 * <p>模拟一通通话：建 CallRecord + 推 open/ASR/close 到 Redis websocket:message:queue。
 * hsc RedisMessageProcessor 消费 → CallTranscriptListener 落 dialog_record + WS REALTIME_DIALOG → 前端工作台。
 */
@Tag(name = "临时测试")
@RestController
@RequestMapping("/test")
@Slf4j
@RequiredArgsConstructor
public class TestController extends BaseController {

    private static final String QUEUE_KEY = "websocket:message:queue";

    private final StringRedisTemplate stringRedisTemplate;
    private final ICallRecordService callRecordService;
    private final ISipAgentService sipAgentService;

    /**
     * 模拟一通完整通话的 ASR 流（open → user 说 → agent 说 → close）。
     *
     * @param agentNumber  坐席分机号（必须在 sys_sip_agent 表，WS 推送按它定位坐席 sessionId）
     * @param callerNumber 模拟主叫号（默认测试号）
     */
    @Operation(summary = "模拟旁路ASR完整通话流(约20秒)", method = "GET")
    @GetMapping("/simulate/session")
    public Object simulateSession(
            @RequestParam String agentNumber,
            @RequestParam(defaultValue = "13800000000") String callerNumber) {

        // 查坐席（WS 推送需要 agentId/agentName + agentNumber 定位 sessionId）
        SipAgentVo agent = sipAgentService.getInfoByAgent(agentNumber);
        if (agent == null) {
            return success("坐席不存在：agentNumber=" + agentNumber);
        }

        // 生成 callId + 建 CallRecord（WS 推送 resolveSessionIdByCallId 要它）
        String callId = "TEST" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_FORMAT)
                + IdUtil.fastSimpleUUID().substring(0, 6);
        CallRecord record = new CallRecord();
        record.setCallId(callId);
        record.setCallerNumber(callerNumber);
        record.setCalleeNumber(agentNumber);
        record.setAgentId(agent.getId());
        record.setAgentNumber(agent.getAgentNumber());
        record.setAgentName(agent.getName());
        record.setDirection(2);
        record.setCallStartTime(new Date());
        record.setCallState(1);
        record.setAnswerFlag(1);
        callRecordService.save(record);
        log.info("模拟通话建 CallRecord: callId={}, agent={}, caller={}", callId, agentNumber, callerNumber);

        // 异步推 Redis（模拟 ai-callbot 推 ASR）
        new Thread(() -> pushSimulatedData(callId, agentNumber, callerNumber)).start();

        return success(callId);
    }

    /**
     * 模拟 ai-callbot 推 Redis 队列（open → ASR 对话 → close）。
     */
    private void pushSimulatedData(String callId, String agentNumber, String callerNumber) {
        String userParams = buildParams("user", callId, agentNumber, callerNumber);
        String agentParams = buildParams("agent", callId, agentNumber, callerNumber);

        // open（user + agent 各一路）
        ThreadUtil.sleep(1000);
        push(userParams + "|open");
        ThreadUtil.sleep(1000);
        push(agentParams + "|open");

        // user 说（增量 + 终稿，模拟 FunASR 2pass）
        ThreadUtil.sleep(2000);
        pushAsr(userParams, "你好，我想咨询一下医保报销", false);
        ThreadUtil.sleep(1500);
        pushAsr(userParams, "你好，我想咨询一下医保报销的政策", true);

        // agent 说
        ThreadUtil.sleep(2000);
        pushAsr(agentParams, "您好，请问您想了解哪方面的医保政策", false);
        ThreadUtil.sleep(1500);
        pushAsr(agentParams, "您好，请问您想了解哪方面的医保政策？", true);

        // user 再说
        ThreadUtil.sleep(2000);
        pushAsr(userParams, "主要是住院报销比例", false);
        ThreadUtil.sleep(1500);
        pushAsr(userParams, "主要是住院报销比例是多少", true);

        // close（user + agent）
        ThreadUtil.sleep(2000);
        push(userParams + "|close");
        ThreadUtil.sleep(1000);
        push(agentParams + "|close");

        log.info("模拟通话 ASR 推送完成: callId={}", callId);
    }

    /**
     * 构造 VendorParams 编码（与 ai-callbot redis_publisher 协议一致）。
     */
    private String buildParams(String role, String callId, String agentNumber, String callerNumber) {
        String params = "role=" + role
                + "&callId=" + callId
                + "&agentId=" + agentNumber
                + "&userNumber=" + callerNumber
                + "&source=bypass";
        return VendorParamsCodec.encode(params);
    }

    /**
     * 推一条 ASR 消息（模拟 FunASR 2pass：online 增量 + offline 终稿）。
     */
    private void pushAsr(String encodedParams, String text, boolean isFinal) {
        JSONObject json = new JSONObject();
        json.put("text", text);
        json.put("is_final", isFinal);
        json.put("mode", isFinal ? "2pass-offline" : "2pass-online");
        push(encodedParams + "|" + json.toJSONString());
    }

    /**
     * RPUSH 到 Redis 队列（hsc RedisMessageProcessor BLPOP 消费）。
     */
    private void push(String message) {
        log.info("模拟推送 Redis: {}", message.substring(0, Math.min(message.length(), 120)));
        stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, message);
    }
}
