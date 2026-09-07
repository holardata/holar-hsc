package com.hsc.system.domain.query.dialogue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通话 AI 摘要字段编辑入参（平迁自 reminder_backend HistoryRecordDTO.editHistoryRecord）。
 *
 * <p>对应 hsc CallAiSummary：人工修正全文总结/关键词/角色总结/要点/代办/思维导图。
 */
@Schema
@Data
public class CallAiSummaryEditQuery {

    @Schema(description = "call_ai_summary 主键ID")
    private Long id;

    @Schema(description = "通话ID(未传 id 时按 callId 定位)")
    private String callId;

    @Schema(description = "全文总结")
    private String summary;

    @Schema(description = "关键词")
    private String keywordAbstract;

    @Schema(description = "角色总结")
    private String roleSummary;

    @Schema(description = "要点提炼")
    private String keypointExtract;

    @Schema(description = "代办事项")
    private String todoThings;

    @Schema(description = "思维导图JSON")
    private String markdownJson;
}
