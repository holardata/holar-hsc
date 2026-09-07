package com.hsc.calltask.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 话后小结提交入参：挂断后补记本通通话的处理结果与备注。
 * 前端挂断时拿不到异步落库的 dialLogId，按拨打维度（任务联系人/私海客户任传其一）
 * 取最近一条拨打历史定位。
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "话后小结提交入参")
@Data
public class CallTaskDispositionQuery {

    @Schema(description = "任务联系人ID(任务拨打挂断后补记用)")
    private Long assignmentId;

    @Schema(description = "客户ID(私海拨打挂断后补记用)")
    private Long customerId;

    @Schema(description = "话后结果(sys_config 参数 call.disposition.options 字典的 value)")
    private String disposition;

    @Schema(description = "话后备注")
    private String dispositionRemark;
}
