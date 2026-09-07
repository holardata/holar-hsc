package com.hsc.calltask.domain.query;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 拨打历史查询入参（assignment/customer/task 三维度任传其一）
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "拨打历史查询入参")
@Data
@EqualsAndHashCode(callSuper = true)
public class CallTaskDialLogQuery extends BaseQuery {

    @Schema(description = "任务联系人ID(call_task_assignment.id)")
    private Long assignmentId;

    @Schema(description = "客户ID(customer_seas.id)：客户详情时间线用")
    private Long customerId;

    @Schema(description = "任务ID(call_task.id)")
    private Long taskId;
}
