package com.hsc.system.domain.query.engine;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema
@Data
public class VoiceEngineAddQuery {

    @Schema(description = "主键ID", hidden = true)
    private Long id;

    /**
     * 引擎实例名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "引擎实例名称")
    private String name;

    /**
     * 引擎类型（VoiceEngineTypeEnum.code）
     */
    @NotBlank(message = "引擎类型不能为空")
    @Schema(description = "引擎类型")
    private String engineType;

    /**
     * 连接参数 JSON 字符串（key 与该类型 flatFields 对齐）
     */
    @Schema(description = "连接参数JSON")
    private String config;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

}
