package com.hsc.system.domain.query.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统参数修改入参：仅 id/参数值/备注三字段——键名与参数名不可改，
 * 请求体即便携带也会被后端忽略（无对应字段自然丢弃）。
 *
 * @author pangshuai
 * @date 2026-09-01
 */
@Schema
@Data
public class SysConfigEditQuery {

    @Schema(description = "参数ID")
    @NotNull(message = "参数ID不能为空")
    private Long id;

    @Schema(description = "参数值")
    @NotBlank(message = "参数值不能为空")
    private String configValue;

    @Schema(description = "备注(参数含义说明)")
    private String remark;
}
