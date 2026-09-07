package com.hsc.ai.service;

import java.util.Map;

/**
 * LLM 调用配置提供者（依赖倒置接口）。
 *
 * <p>hsc-ai 作为底层 LLM 网关，本身不持有任何业务数据。摘要生成 Service 等需要在 hsc-ai 内部完成
 * "取默认模型 + 取提示词"编排的场景，通过本接口获取配置——接口由业务层 hsc-system 实现，
 * 从而避免 hsc-ai 反向依赖 hsc-system，维持 hsc-ai 的底层分层（业务→基础设施，而非反之）。
 */
public interface ILlmConfigProvider {

    /**
     * 组装默认模型的调用配置。
     *
     * <p>返回的 Map 至少含 {@code model}(基础模型名)、{@code base_url}(OpenAI 兼容地址)，
     * 以及 model_config.config 中的附加参数({@code api_key}/temperature/max_tokens 等)，
     * 可直接作为 {@code OpenAiCompatibleClientFactory.chatSync/startChatSyncSimple} 的 conf 入参。
     *
     * @return 默认模型配置 Map；未配置默认模型或默认模型已停用时返回 null
     */
    Map<String, Object> getDefaultModelConf();

    /**
     * 按 title 查询提示词内容。
     *
     * @param title 提示词标题(如 "全文总结"，取值见 {@code com.hsc.ai.constant.PromptTitle})
     * @return 提示词内容；title 为空或不存在返回 null
     */
    String getPromptContent(String title);
}
