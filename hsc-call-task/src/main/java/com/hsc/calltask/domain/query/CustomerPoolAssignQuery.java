package com.hsc.calltask.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 公海客户分配入参（管理员分配制：客户进坐席私海，归属唯一）
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "公海客户分配入参")
@Data
public class CustomerPoolAssignQuery {

    @NotNull(message = "坐席ID不能为空")
    @Schema(description = "目标坐席ID(sip_agent.id)")
    private Long agentId;

    @NotEmpty(message = "客户ID不能为空")
    @Schema(description = "客户ID列表(单个/批量)")
    private List<Long> customerIds;
}
