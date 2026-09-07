package com.hsc.system.domain.query.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体提示词新增/修改入参
 */
@Schema
@Data
public class ChatPromptwordAddQuery {

    @Schema(description = "主键ID", hidden = true)
    private Long id;

    @NotBlank(message = "提示词标题不能为空")
    @Schema(description = "提示词标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "提示词内容")
    private String content;

    @Schema(description = "备注")
    private String remark;
}
