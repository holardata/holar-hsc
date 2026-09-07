package com.hsc.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.ai.service.IHolargptService;
import com.hsc.common.event.AgentRecommendEvent;
import com.hsc.common.event.CallStatusEvent;
import com.hsc.common.event.DialogUpdatedEvent;
import com.hsc.common.event.RedisQueueMessageEvent;
import com.hsc.common.thread.ThreadFactoryImpl;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.entity.HumanAgentConfig;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.IDialogRecordService;
import com.hsc.system.service.IHumanAgentConfigService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通话转写消费者：监听 {@link RedisQueueMessageEvent}，按消息体类型分发处理，
 * 落 dialog_record 并发 {@link DialogUpdatedEvent}/{@link CallStatusEvent} 驱动下游(WebSocket 推送)。
 *
 * <p>平迁自 reminder_backend RedisMessageProcessor 的业务处理逻辑，适配 hsc 的 ApplicationEvent 机制。
 *
 * <p>并发幂等：hsc 通话生命周期由 ESL 事件权威驱动（CHANNEL_CREATE 建 CallRecord、HANGUP 算结束时间），
 * 故 open/close 不再像 rb 那样用 isAllClosed 补算时间（spec D4：协议时序+幂等）；ASR 落库天然幂等
 * （每条 ASR 消息唯一、dialog_record 自增主键）。
 *
 * <p>异步执行（{@code @Async("asyncTaskExecutor")}），不阻塞 RedisMessageProcessor 的消费线程。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CallTranscriptListener {

    private static final String MSG_OPEN = "open";
    private static final String MSG_CLOSE = "close";
    private static final String MSG_TRANSFER = "transfer";
    private static final String MSG_AI_ANSWER = "ai_answer";
    private static final String CALLEE_UUID_PREFIX = "callee_uuid";
    private static final String CALLER_UUID_PREFIX = "caller_uuid";

    private static final String CHANNEL_CUSTOMER = "客户";
    private static final String CHANNEL_AGENT = "坐席";
    private static final String CHANNEL_AI = "AI智能坐席";
    /** AI 智能推荐话术专属 channel_type（区别 AI 坐席真实回复）：给人工坐席看的参考，
     *  前端"AI推荐"开关按它过滤显示 */
    private static final String CHANNEL_RECOMMEND = "AI智能推荐";

    private final ICallRecordService callRecordService;
    private final IDialogRecordService dialogRecordService;
    private final IHolargptService holargptService;
    private final IHumanAgentConfigService humanAgentConfigService;
    private final ApplicationEventPublisher eventPublisher;

    /** per-callId 防抖：1.2s 窗口内累积的客户终稿(StringBuffer 线程安全，支持同 callId 多线程并发累积) */
    private final Map<String, StringBuffer> pendingCustomerText = new ConcurrentHashMap<>();
    /** per-callId 待执行的防抖任务 */
    private final Map<String, ScheduledFuture<?>> pendingRecommendTask = new ConcurrentHashMap<>();
    /** per-callId 推荐是否正在生成(串行保护：正在生成则跳过新触发，避免多流并发致前端 chunk 交错) */
    private final Set<String> recommendRunning = ConcurrentHashMap.newKeySet();
    /**
     * 推荐专用调度池(防抖触发 + 阻塞跑 holargpt)，与 asyncTaskExecutor 隔离：
     * 避免慢 LLM 拖垮共用线程池里的转写落库/WS 推送等任务。
     */
    private final ScheduledExecutorService recommendScheduler =
            new ScheduledThreadPoolExecutor(4, new ThreadFactoryImpl("agent-recommend-"));
    /** 防抖窗口(ms)：客户停顿超过此时长才真正触发推荐 */
    private static final long RECOMMEND_DEBOUNCE_MS = 1200L;

    @Async("asyncTaskExecutor")
    @EventListener
    public void onMessage(RedisQueueMessageEvent event) {
        String callId = event.getCallId();
        String msgBody = event.getMsgBody();
        if (StrUtil.isBlank(callId)) {
            log.warn("Redis 消息缺少 callId，丢弃: {}", msgBody);
            return;
        }
        try {
            if (MSG_OPEN.equalsIgnoreCase(msgBody)) {
                handleOpen(event);
            } else if (MSG_CLOSE.equalsIgnoreCase(msgBody)) {
                handleClose(event);
            } else if (msgBody != null && (msgBody.startsWith(CALLEE_UUID_PREFIX) || msgBody.startsWith(CALLER_UUID_PREFIX))) {
                handleRecordUuid(event, msgBody);
            } else {
                handleAsrOrTransfer(event);
            }
        } catch (Exception e) {
            log.error("处理 Redis 消息异常 callId={}, msgBody={}", callId, msgBody, e);
        }
    }

    /** open：确认 CallRecord 已由 ESL CHANNEL_CREATE 预建；不存在则告警(违背协议时序)，发 open 状态事件。 */
    private void handleOpen(RedisQueueMessageEvent event) {
        CallRecord record = callRecordService.getByCallId(event.getCallId());
        if (record == null) {
            log.warn("open 消息到达但 CallRecord 不存在(违背协议时序，容错丢弃), callId={}", event.getCallId());
            return;
        }
        // callerNumber 用 CallRecord 的真实客户号(ESL CHANNEL_CREATE 落库)：ai-callbot fork 音频时未带客户号
        // (event.getUserNumber 常为空)→ 前端工作台兜底到 SIP remote_identity(转接场景为 unknown)。
        // 与话单侧(CallRecord.callerNumber)统一来源,保证工作台/话单客户号一致。
        eventPublisher.publishEvent(new CallStatusEvent(this, event.getCallId(), MSG_OPEN, event.getAgentId(), record.getCallerNumber()));
    }

    /** close：旁路 ASR leg 结束信号；通话真正结束由 ESL HANGUP 触发(1.12)，此处仅发 close 状态事件。 */
    private void handleClose(RedisQueueMessageEvent event) {
        eventPublisher.publishEvent(new CallStatusEvent(this, event.getCallId(), MSG_CLOSE, event.getAgentId(), event.getUserNumber()));
    }

    /** 录音 uuid（callee_uuid{path}/caller_uuid）：callee_uuid 消息携带录音路径，解析 {...} 回填 CallRecord.filePath（补 1.18）。 */
    private void handleRecordUuid(RedisQueueMessageEvent event, String msgBody) {
        String callId = event.getCallId();
        log.info("收到录音 uuid callId={}, body={}", callId, msgBody);
        // 仅 callee_uuid 消息携带录音文件路径（AI 通话旁路录音）；caller_uuid 是主叫 uuid，非录音路径
        if (msgBody == null || !msgBody.startsWith(CALLEE_UUID_PREFIX)) {
            return;
        }
        String path = extractBraceContent(msgBody);
        if (StrUtil.isBlank(path)) {
            return;
        }
        CallRecord record = callRecordService.getByCallId(callId);
        if (record == null || record.getId() == null) {
            log.warn("录音挂载：CallRecord 不存在 callId={}", callId);
            return;
        }
        // 仅在 filePath 未填充时回填（ESL 链路已填的不覆盖）
        if (StrUtil.isBlank(record.getFilePath())) {
            CallRecord up = new CallRecord();
            up.setId(record.getId());
            up.setFilePath(path);
            callRecordService.updateById(up);
            log.info("录音挂载完成 callId={}, filePath={}", callId, path);
        }
    }

    /** 提取 msgBody 中 {...} 内的内容（录音路径/uuid）。 */
    private String extractBraceContent(String s) {
        if (s == null) {
            return null;
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start + 1, end);
        }
        return null;
    }

    /** ASR 文本 / 转接：解析 JSON，落 dialog_record，发实时转写或转接事件。 */
    private void handleAsrOrTransfer(RedisQueueMessageEvent event) {
        String callId = event.getCallId();
        String msgBody = event.getMsgBody();
        JSONObject json;
        try {
            json = JSONObject.parseObject(msgBody);
        } catch (Exception e) {
            log.warn("消息体非合法 JSON，丢弃 callId={}, body={}", callId, msgBody);
            return;
        }

        String mode = json.getString("mode");
        if (MSG_TRANSFER.equalsIgnoreCase(mode)) {
            String text = json.getString("text");
            saveDialog(callId, mapChannelType(event), MSG_TRANSFER, text, msgBody, MSG_TRANSFER);
            eventPublisher.publishEvent(new CallStatusEvent(this, callId, MSG_TRANSFER, event.getAgentId(), event.getUserNumber()));
            return;
        }

        String text = json.getString("text");
        if (StrUtil.isBlank(text)) {
            return;
        }
        text = trimLeadingPunct(text);
        String source = event.getSource();
        // ai-callbot 的 AI 回复（source=ai & role=agent）落 ai_answer + AI智能坐席（区别真人坐席的"坐席"）；
        // 其余按 role 映射 channel_type、msg_type=ASR。source 落库供前端区分 AI段(ai)/人工段(bypass)。
        boolean isAiReply = "ai".equalsIgnoreCase(source) && "agent".equalsIgnoreCase(event.getRole());
        String channelType = isAiReply ? CHANNEL_AI : mapChannelType(event);
        String msgType = isAiReply ? "ai_answer" : "ASR";
        Boolean isFinal = json.getBoolean("is_final");
        boolean finalLine = Boolean.TRUE.equals(isFinal);
        // 仅终稿落库 dialog_record：中间帧(is_final!=true)是 ASR 流式过程中的残缺片段，
        // 落库会污染挂断摘要与话单对话(同一句的多个中间片段+终稿重复)。中间帧只推前端实时显示。
        if (finalLine) {
            saveDialog(callId, channelType, msgType, text, msgBody, source);
            // 只推终稿给前端:ai-callbot 推的是完整终稿整句(非 LLM 逐字流式),
            // 中间帧残缺 + 前端无句号(seq)合并会丢同说话人连续句,故中间帧不推 WS。
            // 只推终稿 + 前端来一条显一条 = 与话单(dialog_record 终稿)一致。
            // callerNumber 每条转写都带(CallRecord.callerNumber):前端工作台晚开错过 CALL_STATUS.open 也能拿到客户号
            CallRecord rec = callRecordService.getByCallId(callId);
            eventPublisher.publishEvent(new DialogUpdatedEvent(this, callId, channelType, text,
                    "overwrite", new Date(), rec != null ? rec.getCallerNumber() : null));
        }
        // 客户 ASR 终稿 → 触发 holargpt 推荐话术（agent-recommendation）。
        // 仅人工通话段(bypass)触发：推荐是给人工坐席看的参考；AI 坐席接听段(source=ai)自己就是 AI，
        // 无需参考也不调智能体（省 holargpt 调用与算力），转人工后的 bypass 段照常触发
        if (CHANNEL_CUSTOMER.equals(channelType) && finalLine && !"ai".equalsIgnoreCase(source)) {
            scheduleAgentRecommend(callId, text);
        }
    }

    /**
     * 客户 ASR 终稿触发推荐(防抖入口)，线程安全：
     * 1) 累积文本到 pendingCustomerText(StringBuffer，支持同 callId 多线程并发累积)；
     * 2) 防抖：1.2s 窗口内连续终稿合并为一次调用(减少调用+带上下文)；
     * 3) 串行：同一 callId 上一条仍在生成则仅累积不排新任务(由 doAgentRecommend 完成后自查续跑)，
     *    避免多流并发致前端 chunk 交错；
     * 4) putIfAbsent 原子占位，防同 callId 并发重复排任务。
     */
    private void scheduleAgentRecommend(String callId, String customerText) {
        // 1) 累积文本(StringBuffer 线程安全；computeIfAbsent 原子取实例，多线程并发 append 不损坏)
        if (StrUtil.isNotBlank(customerText)) {
            pendingCustomerText.computeIfAbsent(callId, key -> new StringBuffer())
                    .append(customerText).append(' ');
        }
        // 3) 正在生成：文本已累积，由 doAgentRecommend 完成后续跑，此处不排新任务
        if (recommendRunning.contains(callId)) {
            return;
        }
        // 已有等待中的防抖任务：文本已累积，任务到期会取走，不重复排
        if (pendingRecommendTask.containsKey(callId)) {
            return;
        }
        // 4) 排防抖任务；putIfAbsent 原子占位，并发下只保留首个，冗余任务取消
        ScheduledFuture<?> task = recommendScheduler.schedule(() -> {
            pendingRecommendTask.remove(callId);
            doAgentRecommend(callId);
        }, RECOMMEND_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        if (pendingRecommendTask.putIfAbsent(callId, task) != null) {
            task.cancel(false);
        }
    }

    /**
     * 实际调用 holargpt 流式推荐：取出累积文本，每个 chunk 发 {@link AgentRecommendEvent}(isFinal=false)，
     * 流结束发收尾事件。在 recommendScheduler 线程内同步阻塞调用(streamChat 阻塞至流式结束)。
     * recommendRunning 串行保护；完成后若期间又有新文本累积则续跑一次(不丢内容)。
     */
    private void doAgentRecommend(String callId) {
        // 串行占位(并发兜底：putIfAbsent 已保证单任务，此处再防极端竞态)
        if (!recommendRunning.add(callId)) {
            return;
        }
        try {
            String customerText = pendingCustomerText.remove(callId).toString().trim();
            if (StrUtil.isBlank(customerText)) {
                return;
            }
            HumanAgentConfig config = humanAgentConfigService.getSingle();
            if (config == null || StrUtil.isBlank(config.getAgentApiUrl())) {
                log.debug("holargpt 未配置 agentApiUrl，跳过推荐 callId={}", callId);
                return;
            }
            String apiToken = config.getAgentApiKey();
            if (StrUtil.isNotBlank(apiToken) && !apiToken.startsWith("Bearer ")) {
                apiToken = "Bearer " + apiToken;
            }
            // 累积完整推荐话术：streamChat 同步阻塞至流式结束，回调内逐 chunk 累积，
            // 流结束后落库 ai_answer(详情页"推荐"查询用)，对齐 reminder 的持久化行为。
            StringBuilder recommendText = new StringBuilder();
            holargptService.streamChat(config.getAgentApiUrl(), apiToken, callId, customerText,
                    chunk -> {
                        eventPublisher.publishEvent(new AgentRecommendEvent(this, callId, chunk, false));
                        recommendText.append(chunk);
                    });
            // 流式结束，发收尾事件
            eventPublisher.publishEvent(new AgentRecommendEvent(this, callId, "", true));
            // 落库完整推荐话术(channel_type=AI智能推荐, msg_type=ai_answer)：实时已推 WS(AGENT_RECOMMEND)，
            // 此处补持久化供话单详情回看（前端"AI推荐"开关按 channel_type 控制显隐）
            String full = recommendText.toString().trim();
            if (StrUtil.isNotBlank(full)) {
                saveDialog(callId, CHANNEL_RECOMMEND, MSG_AI_ANSWER, full, null, "recommend");
            }
        } catch (Exception e) {
            log.error("触发 holargpt 推荐话术失败 callId={}", callId, e);
        } finally {
            recommendRunning.remove(callId);
        }
        // 完成后若期间又有新文本累积，续跑一次(串行，不丢内容)
        if (pendingCustomerText.containsKey(callId)) {
            scheduleAgentRecommend(callId, "");
        }
    }

    @PreDestroy
    public void destroy() {
        recommendScheduler.shutdown();
    }

    private void saveDialog(String callId, String channelType, String msgType, String text, String asrJson, String source) {
        DialogRecord dialog = new DialogRecord();
        dialog.setCallId(callId);
        dialog.setChannelType(channelType);
        dialog.setMsgType(msgType);
        dialog.setDialogTxt(text);
        dialog.setAsrJson(asrJson);
        dialog.setSource(source);
        dialog.setSpeakTime(new Date());
        dialog.setStatus(0);
        // 解析 asrJson 音频时间戳(若有)，落 audioStart/audioEnd 供前端音频-文字联动
        Long[] ts = extractAudioTimestamp(asrJson);
        if (ts != null) {
            if (ts[0] != null) {
                dialog.setAudioStart(ts[0]);
            }
            if (ts[1] != null) {
                dialog.setAudioEnd(ts[1]);
            }
        }
        dialogRecordService.save(dialog);
    }

    /** 去除 ASR 文本前导标点(逗号/问号/句号等)：FunASR 终稿有时以标点开头(如"，这是啥")，无意义且观感差。
     *  落库 + 推 WS 前统一清洗,话单/实时都不带前导标点。 */
    private static final java.util.regex.Pattern LEADING_PUNCT =
            java.util.regex.Pattern.compile("^[\\s，。？！,\\.\\?!;:、~～…·]+");
    private String trimLeadingPunct(String s) {
        if (StrUtil.isBlank(s)) {
            return s;
        }
        return LEADING_PUNCT.matcher(s).replaceFirst("");
    }

    /** 从 asrJson 解析音频时间戳(毫秒)，兼容 beginTime/endTime 等常见字段；无则返回 null（前端降级）。
     *  AI 回复播报时长(speak_ms)：只落 audioEnd 不落 audioStart——audioStart 有值会触发前端
     *  音频-文字联动(播放高亮/点击跳转)，AI 回复无流内起止位置，刻意留空，时长取 audioEnd 即可。 */
    private Long[] extractAudioTimestamp(String asrJson) {
        if (StrUtil.isBlank(asrJson)) {
            return null;
        }
        try {
            JSONObject json = JSONObject.parseObject(asrJson);
            Long begin = firstLong(json, "beginTime", "begin_time", "startTime", "start_time", "start");
            Long end = firstLong(json, "endTime", "end_time", "stopTime", "stop_time", "end", "stop");
            if (begin != null || end != null) {
                return new Long[]{ begin != null ? begin : 0L, end != null ? end : 0L };
            }
            Long speakMs = firstLong(json, "speakMs", "speak_ms");
            if (speakMs != null) {
                return new Long[]{ null, speakMs };
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long firstLong(JSONObject json, String... keys) {
        for (String k : keys) {
            Long v = json.getLong(k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /** role → channel_type 映射：user→客户、agent→坐席、其他→AI智能坐席。 */
    private String mapChannelType(RedisQueueMessageEvent event) {
        String role = event.getRole();
        if ("user".equalsIgnoreCase(role)) {
            return CHANNEL_CUSTOMER;
        }
        if ("agent".equalsIgnoreCase(role)) {
            return CHANNEL_AGENT;
        }
        return CHANNEL_AI;
    }
}
