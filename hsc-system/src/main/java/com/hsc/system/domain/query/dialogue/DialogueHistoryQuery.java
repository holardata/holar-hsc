package com.hsc.system.domain.query.dialogue;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通话对话历史分页查询入参（平迁自 reminder_backend DialogueController.historyPage）。
 *
 * <p>基于 CallRecord 过滤，关联 CallAiSummary 摘要状态与 dialog_record 句数统计。
 */
@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class DialogueHistoryQuery extends BaseQuery {

    @Schema(description = "通话ID")
    private String callId;

    @Schema(description = "主叫号码")
    private String callerNumber;

    @Schema(description = "被叫号码")
    private String calleeNumber;

    @Schema(description = "坐席工号")
    private String agentNumber;

    @Schema(description = "坐席名称")
    private String agentName;

    @Schema(description = "呼叫方式 1-呼出 2-呼入")
    private Integer direction;
}
