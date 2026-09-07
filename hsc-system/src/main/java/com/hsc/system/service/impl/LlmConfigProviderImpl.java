package com.hsc.system.service.impl;

import com.hsc.ai.service.ILlmConfigProvider;
import com.hsc.system.domain.entity.ChatPromptword;
import com.hsc.system.domain.entity.ModelConfig;
import com.hsc.system.service.IChatPromptwordService;
import com.hsc.system.service.IModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * {@link ILlmConfigProvider} 的业务层实现。
 *
 * <p>从 hsc-system 的 model_config(默认模型) / chat_promptword(提示词) 取数据，组装成 hsc-ai 可直接使用的 conf Map，
 * 供 hsc-ai 的摘要 Service 调用。依赖倒置：hsc-ai 不反向依赖 hsc-system，由本类在业务层提供配置。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LlmConfigProviderImpl implements ILlmConfigProvider {

    private final IModelConfigService modelConfigService;
    private final IChatPromptwordService chatPromptwordService;

    @Override
    public Map<String, Object> getDefaultModelConf() {
        ModelConfig defaultModel = modelConfigService.getDefaultModel();
        // 复用 ModelConfigService.buildConf 统一组装（model/base_url + config 附加参数），避免重复逻辑
        return defaultModel == null ? null : modelConfigService.buildConf(defaultModel);
    }

    @Override
    public String getPromptContent(String title) {
        if (title == null || title.isEmpty()) {
            return null;
        }
        ChatPromptword promptword = chatPromptwordService.getByTitle(title);
        return promptword == null ? null : promptword.getContent();
    }
}
