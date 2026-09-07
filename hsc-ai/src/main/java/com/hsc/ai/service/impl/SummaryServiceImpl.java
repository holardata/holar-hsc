package com.hsc.ai.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.ai.client.OpenAiCompatibleClientFactory;
import com.hsc.ai.constant.PromptTitle;
import com.hsc.ai.service.ILlmConfigProvider;
import com.hsc.ai.service.ISummaryService;
import com.openai.models.chat.completions.ChatCompletion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * 通话摘要生成 Service 实现。平迁自 reminder_backend HolarAiUtil.getAIAnswer。
 *
 * <p>编排：按 title 取提示词({@link ILlmConfigProvider}) → 取默认模型配置 →
 * {@link OpenAiCompatibleClientFactory} 同步调用 → 最多重试 3 次。
 * 失败 / 无配置返回占位文案，不抛异常（摘要失败不应中断主通话流程）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SummaryServiceImpl implements ISummaryService {

    private static final String PLACEHOLDER_NO_MODEL = "接口出现异常";
    private static final String PLACEHOLDER_NO_PROMPT = "没有提示词";
    private static final int RETRY_TIMES = 3;

    private final OpenAiCompatibleClientFactory openAiCompatibleClientFactory;
    private final ILlmConfigProvider llmConfigProvider;

    @Override
    public String generate(String callId, String title, String dialogText) {
        String sid = StrUtil.isBlank(callId) ? UUID.fastUUID().toString() : callId;

        // ① 按 title 取提示词
        String prompt = null;
        if (StrUtil.isNotBlank(title)) {
            prompt = llmConfigProvider.getPromptContent(title);
            if (prompt == null) {
                log.warn("数据库中查询不到 title={} 对应的提示词, sid={}", title, sid);
                return PLACEHOLDER_NO_PROMPT;
            }
        }

        // ② 取默认模型配置
        Map<String, Object> conf = llmConfigProvider.getDefaultModelConf();
        if (conf == null) {
            log.warn("未配置默认模型或默认模型已停用, title={}, sid={}", title, sid);
            return PLACEHOLDER_NO_MODEL;
        }
        log.info("调用LLM title={}, sid={}", title, sid);

        // ③ 调用(失败/空正文重试，最多 3 次)
        int retryTimes = RETRY_TIMES;
        String aiAnswer = PLACEHOLDER_NO_MODEL;
        boolean isOk = false;
        do {
            try {
                ChatCompletion completion = openAiCompatibleClientFactory.startChatSyncSimple(
                        conf, prompt == null ? "" : prompt, dialogText);
                Optional<String> text = openAiCompatibleClientFactory.extractMessageText(completion);
                if (text.isPresent() && StrUtil.isNotBlank(text.get())) {
                    aiAnswer = text.get();
                    isOk = true;
                } else {
                    log.warn("AI服务响应无正文 title={}, sid={}", title, sid);
                    aiAnswer = PLACEHOLDER_NO_MODEL;
                    ThreadUtil.sleep(1000L);
                }
            } catch (Exception ex) {
                log.error("AI服务请求异常 title={}, 重试剩余: {}, sid={}, err={}", title, retryTimes - 1, sid, ex.getMessage(), ex);
                aiAnswer = PLACEHOLDER_NO_MODEL;
                ThreadUtil.sleep(1000L);
            }
        } while (!isOk && (--retryTimes) > 0);
        return aiAnswer;
    }

    @Override
    public String generateXmindJson(String callId, String dialogText) {
        String sid = StrUtil.isBlank(callId) ? UUID.fastUUID().toString() : callId;
        // 对齐 reminder：脑图输入截断到 1000 字，避免超长对话撑爆 LLM 上下文
        String input = StrUtil.isBlank(dialogText) ? "" :
                (dialogText.length() > 1000 ? dialogText.substring(0, 1000) : dialogText);
        String json = stripJsonCodeFence(generate(callId, PromptTitle.XMIND_JSON, input));
        if (isValidXmind(json)) {
            return json;
        }
        log.warn("脑图JSON解析失败，尝试JSON格式优化修正 sid={}", sid);
        String fixed = stripJsonCodeFence(generate(callId, PromptTitle.FIX_JSON, json));
        if (isValidXmind(fixed)) {
            return fixed;
        }
        log.warn("脑图JSON修正仍失败，兜底最小结构 sid={}", sid);
        return "{\"topic\":\"总结\",\"children\":[]}";
    }

    @Override
    public String generateRoleSummary(String callId, String dialogText) {
        String out = stripJsonCodeFence(generate(callId, PromptTitle.ROLE_SUMMARY, dialogText));
        // LLM 偶发漏掉最外层 [ ]（输出 {..},{..}），补上后即为合法 JSON 数组
        String normalized = (out.startsWith("{") && out.contains("},{"))
                ? "[" + out + "]"
                : out;
        if (isValidRoleArray(normalized)) {
            return normalized;
        }
        // 极端情况(截断/混入解释文字)：降级为纯文本总结，前端有纯文本兜底渲染
        log.warn("角色总结JSON规整失败，降级纯文本 callId={}", callId);
        return out;
    }

    /** 校验是否为合法的 {@code [{roleName, roleSummary}]} 数组结构(前端角色卡片渲染所需)。 */
    private boolean isValidRoleArray(String s) {
        if (StrUtil.isBlank(s)) {
            return false;
        }
        try {
            return JSON.parseArray(s) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 剥离 LLM 输出常见的 {@code ```json ... ```} 代码块包裹，并清理转义字符。
     * 脑图提示词的输出示例本身用 {@code ```json} 包裹，LLM 几乎必然模仿，故落库前必须剥离，
     * 否则前端 {@code JSON.parse("```json...```")} 必然失败。
     */
    private String stripJsonCodeFence(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.startsWith("```")) {
            // 去掉首行 ```json / ``` 语言标记
            int firstNl = s.indexOf('\n');
            if (firstNl >= 0) {
                s = s.substring(firstNl + 1);
            }
            int lastFence = s.lastIndexOf("```");
            if (lastFence >= 0) {
                s = s.substring(0, lastFence);
            }
        }
        return s.replace("\\n", "").replace("\\\"", "\"").trim();
    }

    /** 校验是否为合法的 {@code {topic, children}} 脑图结构(jsmind 渲染所需)。 */
    private boolean isValidXmind(String s) {
        if (StrUtil.isBlank(s)) {
            return false;
        }
        try {
            JSONObject obj = JSONObject.parseObject(s);
            return obj != null && obj.containsKey("topic");
        } catch (Exception e) {
            return false;
        }
    }
}
