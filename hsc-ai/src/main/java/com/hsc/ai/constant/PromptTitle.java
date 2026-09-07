package com.hsc.ai.constant;

/**
 * chat_promptword 表提示词 title 常量（平迁自 reminder_backend HolarAiUtil.TOKEN_*）。
 *
 * <p>{@code ISummaryService.generate} 第二个参数 title 取这些常量值，按 title 从 chat_promptword 取提示词。
 * 收口于此，避免调用方散落硬编码中文字符串。
 */
public final class PromptTitle {

    private PromptTitle() {
    }

    public static final String TRANS_ZH_EN = "AI翻译";
    public static final String AI_GAIXIE = "AI改写";
    public static final String KEYWORD_ABSTRACT = "关键词提取";
    public static final String ROLE_SUMMARY = "角色总结";
    public static final String KEYPOINT_EXTRACT = "要点提炼";
    public static final String TODO_THINGS = "代办事项";
    public static final String XMIND_JSON = "思维导图";
    public static final String OVERALL_SUMMARY = "全文总结";
    public static final String FIX_JSON = "JSON格式优化";
    /** 过程摘要（坐席助手实时摘要：定时 2 分钟 + 挂断兜底共用） */
    public static final String PROCESS_SUMMARY_CONTENT = "过程摘要";
    /** 过程摘要标题 */
    public static final String PROCESS_SUMMARY_TITLE = "过程摘要标题";
}
