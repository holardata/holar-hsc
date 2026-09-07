// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.call;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "拨打入参")
@Data
public class CallQuery extends BaseQuery {

    @NotNull(message = "坐席ID不能为空")
    @Schema(description = "坐席ID")
    private Long agentId;

    @Schema(description = "员工ID")
    private Long userId;

    @Schema(description = "主叫",hidden = true)
    private String caller;

    @NotEmpty(message = "被叫号码不能为空")
    @Schema(description = "被叫")
    private String callee;

    @Schema(description = "主叫超时时间,默认10秒")
    private Integer callerTimeOut = 10;


    @Schema(description = "被叫超时时间(秒)。null=运行时读系统参数 call.ring-timeout.outbound(参数页可调,默认25,被叫是客户手机响铃15~25秒接听常见)")
    private Integer calleeTimeOut;


    @Schema(description = "是否隐藏客户号码(0-不隐藏 1-隐藏)")
    private Integer hiddenCustomer = 0;

    @Schema(description = "任务联系人ID(call_task_assignment.id)：座机从任务待拨列表代拨时携带，"
            + "用于挂断回写任务联系人；手动代拨不传（软电话路径经 SIP 头携带，不走本字段）")
    private Long assignmentId;

    @Schema(description = "客户ID(customer_seas.id)：私海维度代拨（我的客户发起）携带，"
            + "校验归属坐席本人后随 CallInfo 持有，挂断记客户维度外呼；手动代拨不传")
    private Long customerId;

}
