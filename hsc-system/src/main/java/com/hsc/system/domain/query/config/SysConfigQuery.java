package com.hsc.system.domain.query.config;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 系统参数分页查询
 *
 * @author pangshuai
 * @date 2026-09-01
 */
@Schema
@Data
public class SysConfigQuery extends BaseQuery {

    @Schema(description = "参数名(模糊)")
    private String configName;

    @Schema(description = "参数键(模糊)")
    private String configKey;
}
