package com.hsc.calltask.domain.entity;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;


/**
 * 外呼拨打历史表(CallTaskDialLog)表实体类：一行=一次真实发生并挂断的外呼呼叫（任务拨打/私海拨打）
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("call_task_dial_log")
public class CallTaskDialLog extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 1L;


    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     *  任务ID（私海拨打为空）
     */
    @Schema(description = "任务ID(call_task.id,私海拨打为空)")
    @TableField("task_id")
    private Long taskId;


    /**
     *  任务联系人ID（私海拨打为空）
     */
    @Schema(description = "任务联系人ID(call_task_assignment.id,私海拨打为空)")
    @TableField("assignment_id")
    private Long assignmentId;


    /**
     *  拨打坐席
     */
    @Schema(description = "拨打坐席(sip_agent.id)")
    @TableField("agent_id")
    private Long agentId;


    /**
     *  客户ID（无档案为空）
     */
    @Schema(description = "客户ID(customer_seas.id),无档案为空")
    @TableField("customer_id")
    private Long customerId;


    /**
     *  被叫号码快照
     */
    @Schema(description = "被叫号码快照")
    @TableField("phone")
    private String phone;


    /**
     *  话单ID
     */
    @Schema(description = "话单ID(call_record.id)")
    @TableField("call_record_id")
    private Long callRecordId;


    /**
     *  雪花callId（冗余，直查话单用）
     */
    @Schema(description = "雪花callId(冗余,直查话单用)")
    @TableField("call_id")
    private String callId;


    /**
     *  拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败
     */
    @Schema(description = "拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败")
    @TableField("call_result")
    private Integer callResult;


    /**
     *  FS挂机原因码（冗余）
     */
    @Schema(description = "FS挂机原因码(冗余,FsHangupCauseEnum)")
    @TableField("hangup_cause_code")
    private Integer hangupCauseCode;


    /**
     *  FS挂机原因原始串（排障用）
     */
    @Schema(description = "FS挂机原因原始串(排障用)")
    @TableField("hangup_cause")
    private String hangupCause;


    /**
     *  坐席话后结果（参数字典值，挂断后坐席补记）
     */
    @Schema(description = "坐席话后结果(sys_config参数字典值)")
    @TableField("disposition")
    private String disposition;


    /**
     *  话后备注
     */
    @Schema(description = "话后备注")
    @TableField("disposition_remark")
    private String dispositionRemark;


    /**
     *  呼叫发起时间
     */
    @Schema(description = "呼叫发起时间(话单callStartTime)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("dial_time")
    private Date dialTime;


    /**
     *  通话时长（秒，未接通为0）
     */
    @Schema(description = "通话时长(秒,未接通为0)")
    @TableField("talk_duration")
    private Integer talkDuration;


}
