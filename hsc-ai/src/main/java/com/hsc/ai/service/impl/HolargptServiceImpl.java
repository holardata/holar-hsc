package com.hsc.ai.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.ai.service.IHolargptService;
import io.netty.handler.codec.http.HttpHeaders;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * holargpt 智能体 SSE 流式调用实现（平迁自 reminder_backend ChatAsyncHttpClient + ChatGptSubscriber）。
 *
 * <p>SSE 协议(holargpt/FastGPT)：
 * <pre>
 * event: answer
 * data: {"choices":[{"delta":{"content":"xxx"},"finish_reason":null}]}
 * data: [DONE]
 * </pre>
 * 每个 data 行解析 choices[0].delta.content 增量 chunk，回调 onChunk。
 *
 * <p>实现用 asynchttpclient 流式（onBodyPartReceived 累积 buffer，按行解析 SSE），比 rb 的 reactive
 * StreamedAsyncHandler 简化但功能对等。
 */
@Slf4j
@Service
public class HolargptServiceImpl implements IHolargptService {

    private static final String DATA_PREFIX = "data:";
    private static final String FINISH_MARK = "[DONE]";

    private final AsyncHttpClient client = Dsl.asyncHttpClient();

    @Override
    public void streamChat(String apiUrl, String apiToken, String chatId, String userText, Consumer<String> onChunk) {
        if (StrUtil.isBlank(apiUrl)) {
            log.warn("holargpt streamChat 跳过：apiUrl 为空 chatId={}", chatId);
            return;
        }
        if (apiUrl.trim().toLowerCase().startsWith("https")) {
            log.error("holargpt 流式接口必须用 http(https 会丢信息) apiUrl={}", apiUrl);
            return;
        }
        JSONObject body = buildBody(chatId, userText);
        StringBuilder buffer = new StringBuilder();
        log.info("holargpt streamChat 开始 callId={}, textLen={}", chatId, userText == null ? 0 : userText.length());
        try {
            client.preparePost(apiUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", apiToken == null ? "" : apiToken)
                    .addHeader("Accept", "text/event-stream")
                    .setBody(body.toJSONString())
                    .execute(new AsyncHandler<Response>() {
                        @Override
                        public State onStatusReceived(HttpResponseStatus status) {
                            return status.getStatusCode() == 200 ? State.CONTINUE : State.ABORT;
                        }

                        @Override
                        public State onHeadersReceived(HttpHeaders headers) {
                            return State.CONTINUE;
                        }

                        @Override
                        public State onBodyPartReceived(HttpResponseBodyPart part) {
                            buffer.append(new String(part.getBodyPartBytes(), StandardCharsets.UTF_8));
                            drainSse(buffer, onChunk);
                            return State.CONTINUE;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            log.error("holargpt SSE 异常 callId={}", chatId, t);
                        }

                        @Override
                        public Response onCompleted() {
                            log.info("holargpt streamChat 完成 callId={}", chatId);
                            return null;
                        }
                    }).get();
        } catch (Exception e) {
            log.error("holargpt streamChat 调用失败 callId={}", chatId, e);
        }
    }

    /** 构造 holargpt/FastGPT 流式请求体。 */
    private JSONObject buildBody(String chatId, String userText) {
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", userText);
        JSONArray messages = new JSONArray();
        messages.add(msg);

        JSONObject variables = new JSONObject();
        variables.put("cTime", DateUtil.now());

        JSONObject body = new JSONObject();
        body.put("chatId", chatId);
        body.put("detail", true);
        body.put("stream", true);
        body.put("messages", messages);
        body.put("variables", variables);
        return body;
    }

    /**
     * 解析 buffer 中完整的 SSE 行（以 \n 分隔），提取 data 行的 delta.content 调 onChunk。
     * 不完整行（无 \n）保留在 buffer 等下次 bodyPart。
     */
    private void drainSse(StringBuilder buffer, Consumer<String> onChunk) {
        int nl;
        while ((nl = buffer.indexOf("\n")) >= 0) {
            String line = buffer.substring(0, nl);
            buffer.delete(0, nl + 1);
            line = line.trim();
            if (!line.startsWith(DATA_PREFIX)) {
                continue;
            }
            String data = line.substring(DATA_PREFIX.length()).trim();
            if (data.isEmpty() || FINISH_MARK.equals(data)) {
                continue;
            }
            String content = extractDeltaContent(data);
            if (StrUtil.isNotBlank(content) && onChunk != null) {
                onChunk.accept(content);
            }
        }
    }

    private String extractDeltaContent(String data) {
        try {
            JSONObject obj = JSON.parseObject(data);
            JSONArray choices = obj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            return delta == null ? null : delta.getString("content");
        } catch (Exception e) {
            log.debug("holargpt SSE 行非预期 JSON: {}", data);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (client != null && !client.isClosed()) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 holargpt AsyncHttpClient 异常", e);
            }
        }
    }
}
