package com.hsc.system.domain.query.ai;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能体提示词分页查询入参
 */
@Schema
@Data
public class ChatPromptwordQuery extends BaseQuery {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "主键ID集合(批量删除)")
    private List<Long> ids;

    @Schema(description = "提示词标题")
    private String title;
}
