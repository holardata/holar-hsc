package com.hsc.system.domain.query.ai;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * LLM 模型配置分页查询入参
 */
@Schema
@Data
public class ModelConfigQuery extends BaseQuery {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "主键ID集合(批量删除)")
    private List<Long> ids;

    @Schema(description = "模型配置名称")
    private String name;

    @Schema(description = "基础模型名")
    private String baseModel;

    @Schema(description = "状态 0-停用 1-启用")
    private Integer status;

    @Schema(description = "是否默认 0-否 1-是")
    private Integer isDefault;
}
