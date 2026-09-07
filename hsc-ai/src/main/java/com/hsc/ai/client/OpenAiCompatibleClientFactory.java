package com.hsc.ai.client;

import cn.hutool.core.util.StrUtil;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.core.Timeout;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 通用 OpenAI 兼容 ChatClient 工厂。
 *
 * 平迁自 reminder_backend 的 LlmClientUtil，适配 hsc-ai（Java 17 + Spring Boot 3）。
 * 支持运行时动态 baseUrl + model + apiKey 调用任意 OpenAI 兼容接口（自建网关 / DeepSeek / Qwen 等国产模型）。
 * 客户端按 baseUrl|apiKey 缓存，模型配置变更后须调 {@link #evictSyncClient} 清理。
 *
 * 与固定厂商的 dashscopeClient 并行，各司其职：
 * - dashscopeClient（spring-ai 自动配置）：固定 DashScope 调用
 * - 本工厂（model_config 表驱动）：运营管理的动态模型，供摘要/关键词/翻译等结构化任务调用
 *
 * 调用入口：摘要生成 Service 等注入本 Bean，调 {@link #chatSync}。
 */
@Slf4j
@Component
public class OpenAiCompatibleClientFactory {

    /** chat/completions 可识别的采样/长度等参数键（透传上游，过滤无关键）；含 chat_template_kwargs 以支持关闭思考及模型特有模板参数。 */
    public static final Set<String> ALL_CHAT_PARAM = new HashSet<>(Arrays.asList(
            "n", "best_of", "presence_penalty", "frequency_penalty", "repetition_penalty",
            "temperature", "top_p", "top_k", "min_p", "seed", "stop", "stop_token_ids",
            "bad_words", "include_stop_str_in_output", "ignore_eos", "max_tokens", "min_tokens",
            "logprobs", "prompt_logprobs", "detokenize", "skip_special_tokens",
            "spaces_between_special_tokens", "logits_processors", "truncate_prompt_tokens",
            "guided_decoding", "logit_bias", "allowed_token_ids", "extra_args",
            "chat_template_kwargs", "response_format"));

    /** 按 baseUrl|apiKey 缓存同步客户端。单例 Bean，进程内共享。 */
    private final ConcurrentMap<String, OpenAIClient> syncClientCache = new ConcurrentHashMap<>();

    /** 按 baseUrl|apiKey 缓存同步客户端；模型配置变更后须调用 evictSyncClient 清理。 */
    public OpenAIClient getOrCreateSyncClient(String apiKey, String baseUrl) {
        String cacheKey = baseUrl + "|" + apiKey;
        return syncClientCache.computeIfAbsent(cacheKey, k -> {
            log.info("创建新的 OpenAIClient 实例, baseUrl: {}", baseUrl);
            return OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .timeout(Timeout.builder().connect(Duration.ofSeconds(60)).read(Duration.ofSeconds(300)).build())
                    .build();
        });
    }

    /** 清理指定 baseUrl|apiKey 的同步客户端缓存（模型配置保存/更新/删除/启停后调用，使新配置下次生效）。 */
    public void evictSyncClient(String apiKey, String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return;
        }
        String cacheKey = baseUrl + "|" + (apiKey == null ? "" : apiKey);
        syncClientCache.remove(cacheKey);
    }

    /**
     * 简单同步对话（system + user），返回正文文本；正文为空时返回 null。
     * conf 需含：model（或 base_model）、base_url（或 api_domain）、api_key，及可选采样参数。
     */
    public String chatSync(Map<String, Object> conf, String systemPrompt, String userMessage) {
        ChatCompletion completion = startChatSyncSimple(conf, systemPrompt, userMessage);
        return extractMessageText(completion).orElse(null);
    }

    /**
     * 无 JSON Schema 的简单同步对话（仅 system + user），用于摘要/关键词/翻译等纯文本输出。
     * conf 需含：model（或 base_model）、base_url（或 api_domain）、api_key，及可选采样参数。
     */
    public ChatCompletion startChatSyncSimple(Map<String, Object> conf, String systemPrompt, String userMessage) {
        Map<String, ? extends JsonValue> baseParam = buildChatBodyParams(conf);
        String model = str(conf, "model", "base_model", "baseModel");
        if (StrUtil.isBlank(model)) {
            throw new IllegalStateException("模型配置缺少 model/base_model");
        }
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .additionalBodyProperties(baseParam)
                .addSystemMessage(systemPrompt == null ? "" : systemPrompt)
                .addUserMessage(userMessage == null ? "" : userMessage)
                .build();
        OpenAIClient client = resolveSyncClient(conf);
        return client.chat().completions().create(params);
    }

    /**
     * 连通性测试：用独立短超时客户端发一条受 maxTokens 限制的消息，快速验证模型可用性。
     * 不复用业务客户端缓存（业务调用需长 read 超时），readTimeoutSec 控制等待模型响应的最长时间。
     */
    public ChatCompletion testChatCompletion(Map<String, Object> conf, String userMessage, int maxTokens, long readTimeoutSec) {
        String apiKey = str(conf, "api_key", "apiKey");
        String baseUrl = str(conf, "base_url", "baseUrl", "api_domain", "apiDomain");
        String model = str(conf, "model", "base_model", "baseModel");
        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(baseUrl)) {
            throw new IllegalStateException("模型配置缺少 api_key 或 base_url");
        }
        if (StrUtil.isBlank(model)) {
            throw new IllegalStateException("模型配置缺少 model/base_model");
        }
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(Timeout.builder().connect(Duration.ofSeconds(15)).read(Duration.ofSeconds(readTimeoutSec)).build())
                .build();
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens(maxTokens)
                .addUserMessage(userMessage == null ? "" : userMessage)
                .build();
        return client.chat().completions().create(params);
    }

    /** 从 ChatCompletion 提取正文（choices[0].message.content；为空时尝试 reasoning_content 兜底）。 */
    public Optional<String> extractMessageText(ChatCompletion completion) {
        if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
            return Optional.empty();
        }
        ChatCompletion.Choice choice = completion.choices().get(0);
        if (choice == null || choice.message() == null) {
            return Optional.empty();
        }
        Optional<String> content = choice.message().content();
        if (content.isPresent() && StrUtil.isNotBlank(content.get())) {
            return Optional.of(content.get());
        }
        return extractAdditionalProperties(choice.message())
                .flatMap(add -> extractTextFromAdditionalProperties(add, "reasoning_content", "reasoning", "thinking"));
    }

    // ---------------- 内部辅助 ----------------

    /** 组装请求体：透传采样/长度参数；按 config.enable_thinking 决定是否写 chat_template_kwargs.enable_thinking=false。 */
    private Map<String, ? extends JsonValue> buildChatBodyParams(Map<String, Object> conf) {
        Map<String, Object> requestMap = conf == null ? new HashMap<>() : new HashMap<>(conf);
        if (!resolveThinkingEnabled(requestMap, false)) {
            // 关闭思考：合并而非覆盖，保留用户在 chat_template_kwargs 中配置的其它参数（如 clear_thinking）
            Object rawKwargs = requestMap.get("chat_template_kwargs");
            Map<String, Object> kwargs = new HashMap<>();
            if (rawKwargs instanceof Map<?, ?>) {
                ((Map<?, ?>) rawKwargs).forEach((k, v) -> kwargs.put(String.valueOf(k), v));
            }
            kwargs.put("enable_thinking", false);
            requestMap.put("chat_template_kwargs", kwargs);
        }
        return toJsonValueMap(requestMap, ALL_CHAT_PARAM);
    }

    /** 解析思考开关：优先读 map.enable_thinking（支持 Boolean/Number/String），未配置则用 defaultThink。 */
    private boolean resolveThinkingEnabled(Map<String, Object> map, boolean defaultThink) {
        if (map == null) {
            return defaultThink;
        }
        Object configured = map.get("enable_thinking");
        if (configured == null) {
            return defaultThink;
        }
        if (configured instanceof Boolean) {
            return (Boolean) configured;
        }
        if (configured instanceof Number) {
            return ((Number) configured).intValue() != 0;
        }
        return Boolean.parseBoolean(configured.toString());
    }

    private OpenAIClient resolveSyncClient(Map<String, Object> conf) {
        Object client = conf.get("client");
        if (client instanceof OpenAIClient) {
            return (OpenAIClient) client;
        }
        String apiKey = str(conf, "api_key", "apiKey");
        String baseUrl = str(conf, "base_url", "baseUrl", "api_domain", "apiDomain");
        if (StrUtil.isBlank(apiKey) || StrUtil.isBlank(baseUrl)) {
            throw new IllegalStateException("模型配置缺少 api_key 或 base_url");
        }
        return getOrCreateSyncClient(apiKey, baseUrl);
    }

    private Map<String, ? extends JsonValue> toJsonValueMap(Map<String, Object> input, Set<String> usingKeys) {
        if (input == null) {
            return Collections.emptyMap();
        }
        return input.entrySet().stream()
                .filter(e -> usingKeys.contains(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> JsonValue.from(e.getValue()),
                        (a, b) -> a,
                        HashMap::new));
    }

    private String str(Map<String, Object> conf, String... keys) {
        if (conf == null) {
            return null;
        }
        for (String k : keys) {
            Object v = conf.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private Optional<Map<String, JsonValue>> extractAdditionalProperties(Object holder) {
        if (holder == null) {
            return Optional.empty();
        }
        try {
            Method m = holder.getClass().getMethod("_additionalProperties");
            Object value = m.invoke(holder);
            if (!(value instanceof Map<?, ?>)) {
                return Optional.empty();
            }
            Map<String, JsonValue> add = new HashMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> {
                if (k != null && v instanceof JsonValue) {
                    add.put(String.valueOf(k), (JsonValue) v);
                }
            });
            return add.isEmpty() ? Optional.empty() : Optional.of(add);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractTextFromAdditionalProperties(Map<String, JsonValue> add, String... keys) {
        if (add == null || add.isEmpty()) {
            return Optional.empty();
        }
        for (String k : keys) {
            JsonValue v = add.get(k);
            if (v == null) {
                continue;
            }
            try {
                String s = v.convert(String.class);
                if (StrUtil.isNotBlank(s)) {
                    return Optional.of(s);
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }
}
