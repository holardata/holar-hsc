package com.hsc.system.domain.query.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * LLM 模型配置新增/修改入参
 */
@Schema
@Data
public class ModelConfigAddQuery {

    @Schema(description = "主键ID", hidden = true)
    private Long id;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "基础模型名不能为空")
    @Schema(description = "基础模型名(如 qwen-plus/deepseek-chat)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String baseModel;

    @NotBlank(message = "API地址不能为空")
    @Schema(description = "OpenAI兼容baseUrl", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiDomain;

    @Schema(description = "附加配置JSON(api_key/temperature/max_tokens等)")
    private Map<String, Object> config;

    @Schema(description = "是否默认 0-否 1-是")
    private Integer isDefault;

    @Schema(description = "状态 0-停用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
