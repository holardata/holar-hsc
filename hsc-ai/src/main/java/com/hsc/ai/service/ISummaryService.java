package com.hsc.ai.service;

/**
 * 通话摘要生成 Service。
 *
 * <p>封装"按 title 取提示词 + 取默认模型 + 组装调用 + 重试 + 返回结果"的编排逻辑，
 * 供 call-ai-summary(过程摘要/挂断全文摘要)、agent-recommendation、对话翻译/改写等业务调用。
 * 平迁自 reminder_backend {@code HolarAiUtil.getAIAnswer}。
 *
 * <p>所有 LLM 调用经此(或 {@code OpenAiCompatibleClientFactory})统一出口，业务代码 MUST NOT 直连 LLM SDK。
 */
public interface ISummaryService {

    /**
     * 生成摘要/结构化结果。
     *
     * @param callId     通话ID(仅用于日志追溯，可空)
     * @param title      提示词标题(对应 chat_promptword.title，如 "全文总结"/"关键词提取")
     * @param dialogText 对话文本(作为 user message 喂给 LLM)
     * @return LLM 返回的正文；未配置默认模型/提示词/调用失败时返回占位文案(不抛异常，不中断主流程)
     */
    String generate(String callId, String title, String dialogText);

    /**
     * 生成思维导图 JSON（{@code {topic, children}} 结构，供前端 jsmind 直接渲染）。
     *
     * <p>脑图要求严格 JSON，而 LLM 常以 {@code ```json} 代码块包裹、或返回带语法错误的 JSON。
     * 故相比 {@link #generate} 多一层后处理：剥离代码块 → 解析校验 → 失败用
     * "JSON格式优化"提示词二次修正 → 仍失败兜底为最小 {@code {"topic":"总结","children":[]}}，
     * 确保落库的 markdownJson 始终是可被前端 {@code JSON.parse} 的合法结构。
     * 平迁自 reminder_backend HistoryRecordServiceImpl 脑图后处理逻辑。
     *
     * @param callId     通话ID(仅日志追溯)
     * @param dialogText 对话文本(作为 user message 喂给 LLM)
     * @return 合法的 {@code {topic, children}} JSON 字符串
     */
    String generateXmindJson(String callId, String dialogText);

    /**
     * 生成角色总结 JSON 数组（{@code [{roleName, roleSummary}]} 结构，供前端角色卡片渲染）。
     *
     * <p>提示词要求 LLM 只输出 JSON 数组，但 LLM 偶发漏掉最外层 {@code [ ]}
     * （输出 {@code {..},{..}}），前端 {@code JSON.parse} 失败会把原始 JSON 串当纯文本展示。
     * 故落库前统一规整：剥离代码块 → 缺 {@code [ ]} 包裹时补上 → 解析校验。
     *
     * @param callId     通话ID(仅日志追溯)
     * @param dialogText 对话文本(作为 user message)
     * @return 合法的 {@code [{roleName, roleSummary}]} JSON 数组字符串
     */
    String generateRoleSummary(String callId, String dialogText);
}
