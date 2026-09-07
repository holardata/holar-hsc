package com.hsc.ai.service;

import java.util.function.Consumer;

/**
 * holargpt 智能体 SSE 流式调用服务（平迁自 reminder_backend ChatAsyncHttpClient）。
 *
 * <p>人工坐席通话时，把客户 ASR 终稿喂给 holargpt 智能体，SSE 流式回推推荐话术。
 * 每个增量 chunk 经 {@code onChunk} 回调返回，由调用方负责推送前端
 * （hsc-ai 不依赖 hsc-websocket，推送由调用方经事件 + WsCallPushService 完成）。
 *
 * <p>调用方须在异步线程调用（{@code streamChat} 阻塞至流式结束）；onChunk 在 HTTP IO 线程触发，
 * 回调内应只做轻量动作（如发 ApplicationEvent）。
 */
public interface IHolargptService {

    /**
     * 流式调用 holargpt 智能体。
     *
     * @param apiUrl   holargpt 流式接口地址（必须 http，https 会丢信息）
     * @param apiToken Authorization 头值（含 Bearer 前缀）
     * @param chatId   会话ID（透传 holargpt chatId，等于 hsc callId）
     * @param userText 客户 ASR 终稿文本
     * @param onChunk  每个增量 chunk 回调（delta.content）；可空（仅消费流）
     */
    void streamChat(String apiUrl, String apiToken, String chatId, String userText, Consumer<String> onChunk);
}
