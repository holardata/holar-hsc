package com.hsc.calltask.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 拨打历史明细 Vo（assignment/customer 维度查询，join 任务名/坐席名装饰）
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "拨打历史明细")
@Data
public class CallTaskDialLogVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID(私海拨打为空)")
    private Long taskId;

    @Schema(description = "任务名(私海拨打为空，前端显示私海拨打)")
    private String taskName;

    @Schema(description = "任务联系人ID(私海拨打为空)")
    private Long assignmentId;

    @Schema(description = "拨打坐席ID")
    private Long agentId;

    @Schema(description = "拨打坐席名")
    private String agentName;

    @Schema(description = "客户ID(无档案为空)")
    private Long customerId;

    @Schema(description = "被叫号码快照")
    private String phone;

    @Schema(description = "话单ID(跳转话单详情用)")
    private Long callRecordId;

    @Schema(description = "雪花callId(跳转话单详情用)")
    private String callId;

    @Schema(description = "拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败")
    private Integer callResult;

    @Schema(description = "FS挂机原因原始串(排障)")
    private String hangupCause;

    @Schema(description = "坐席话后结果(参数字典值)")
    private String disposition;

    @Schema(description = "话后备注")
    private String dispositionRemark;

    @Schema(description = "呼叫发起时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dialTime;

    @Schema(description = "通话时长(秒,未接通为0)")
    private Integer talkDuration;
}
