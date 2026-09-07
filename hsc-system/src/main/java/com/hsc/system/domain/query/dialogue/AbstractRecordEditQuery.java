package com.hsc.system.domain.query.dialogue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 过程摘要编辑入参（平迁自 reminder_backend AbstractRecordDTO.editAbstractRecord）。
 */
@Schema
@Data
public class AbstractRecordEditQuery {

    @Schema(description = "abstract_record 主键ID")
    private Long id;

    @Schema(description = "通话ID(编辑后回查该通话过程摘要列表用)")
    private String callId;

    @Schema(description = "原文快照")
    private String dialogTxt;

    @Schema(description = "标题摘要")
    private String title;

    @Schema(description = "内容摘要")
    private String content;
}
