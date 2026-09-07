// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ai.service.impl;


import com.hsc.ai.client.OpenAiCompatibleClientFactory;
import com.hsc.ai.service.IDeepSeekService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek 对话服务。
 *
 * DeepSeek 是 OpenAI 兼容接口，收敛到 {@link OpenAiCompatibleClientFactory} 统一调用，
 * 不再维护独立的 spring-ai deepseekClient Bean（原 Bean 未注册导致启动失败）。
 * 配置来自 application.yml 的 spring.ai.deepseek.*。
 *
 * @author danmo
 * @date 2025/06/10 16:14
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DeepSeekServiceImpl implements IDeepSeekService {

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}")
    private String model;

    private final OpenAiCompatibleClientFactory openAiCompatibleClientFactory;

    @Override
    public String chat(String question) {
        Map<String, Object> conf = new HashMap<>();
        conf.put("base_url", baseUrl);
        conf.put("api_key", apiKey);
        conf.put("model", model);
        return openAiCompatibleClientFactory.chatSync(conf, null, question);
    }
}
