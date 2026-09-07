package com.hsc.calltask.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户流转时间线条目（金蝶订单日志式）：归属动作事件与外呼明细混排
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "客户流转时间线条目")
@Data
public class CustomerTransferTimelineVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "条目类型 1-归属动作(导入/分配/收回) 2-外呼")
    private Integer eventType;

    @Schema(description = "事件时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date eventTime;

    // ===== 归属动作字段（eventType=1）=====
    @Schema(description = "动作 1-导入入库 2-分配 3-收回")
    private Integer action;

    @Schema(description = "原归属坐席名（分配时空）")
    private String fromAgentName;

    @Schema(description = "原归属坐席绑定登录用户名（坐席未绑定或分配时空）")
    private String fromAgentUserName;

    @Schema(description = "新归属坐席名（收回时空）")
    private String toAgentName;

    @Schema(description = "新归属坐席绑定登录用户名（坐席未绑定或收回时空）")
    private String toAgentUserName;

    @Schema(description = "操作管理员名（导入事件为空）")
    private String operatorName;

    @Schema(description = "原因（收回时）")
    private String reason;

    // ===== 外呼字段（eventType=2）=====
    @Schema(description = "拨打历史记录ID（跳转/话后关联用）")
    private Long dialLogId;

    @Schema(description = "任务名（私海拨打为空，前端显示私海拨打）")
    private String taskName;

    @Schema(description = "拨打坐席名")
    private String agentName;

    @Schema(description = "被叫号码")
    private String phone;

    @Schema(description = "雪花callId（跳话单详情用）")
    private String callId;

    @Schema(description = "拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败")
    private Integer callResult;

    @Schema(description = "通话时长(秒,未接通为0)")
    private Integer talkDuration;

    @Schema(description = "坐席话后结果(参数字典值)")
    private String disposition;
}
