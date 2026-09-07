// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
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
 * 任务分配联系人表(CallTaskAssignment)表实体类
 *
 * @author danmo
 * @since 2025-07-14 10:06:14
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("call_task_assignment")
public class CallTaskAssignment extends BaseEntity implements Serializable {
  private static final long serialVersionUID = 926853607531361433L;
   
    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


     
    /**
     *  任务ID 
     */
    @Schema(description = "任务ID")
    @TableField("task_id")
    private Long taskId;
    
    
     
    /**
     *  联系方式
     */
    @Schema(description = "联系方式")
    @TableField("phone")
    private String phone;


    /**
     *  客户名称
     */
    @Schema(description = "客户名称")
    @TableField("name")
    private String name;


    /**
     *  扩展字段
     */
    @Schema(description = "扩展字段")
    @TableField("ext")
    private String ext;
    
    
     
    /**
     *  来源 0-人群导入 1-文件导入 2-API导入 
     */
    @Schema(description = "来源 0-人群导入 1-文件导入 2-API导入")
    @TableField("source")
    private Integer source;

    /**
     *  人群ID
     */
    @Schema(description = "人群ID")
    @TableField("crowd_id")
    private Long crowdId;

    /**
     *  来源客户ID（customer_seas.id），人群导入时有值；文件导入无客户档案留空
     */
    @Schema(description = "来源客户ID(customer_seas.id)")
    @TableField("customer_id")
    private Long customerId;

    /**
     *  模板ID
     */
    @Schema(description = "模板ID")
    @TableField("template_id")
    private Long templateId;
    
    
     
    /**
     *  分配状态 0-未分配 1-已分配 
     */
    @Schema(description = "分配状态 0-未分配 1-已分配")
    @TableField("status")
    private Integer status;
    
    
     
    /**
     *  坐席ID 
     */
    @Schema(description = "坐席ID")
    @TableField("agent_id")
    private Long agentId;
    
    
     
    /**
     *  分配时间 
     */
    @Schema(description = "分配时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("assignment_time")
    private Date assignmentTime;
    
    
     
    /**
     *  拨打状态 0-未拨打 1-已拨打 
     */
    @Schema(description = "拨打状态 0-未拨打 1-已拨打")
    @TableField("call_status")
    private Integer callStatus;

    /**
     *  最近拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败（NULL=未拨打或历史未知）
     */
    @Schema(description = "最近拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败(NULL=未拨打或历史未知)")
    @TableField("call_result")
    private Integer callResult;

    /**
     *  最后拨打完成时间（挂断回写）
     */
    @Schema(description = "最后拨打完成时间(挂断回写)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("last_dial_time")
    private Date lastDialTime;

    /**
     *  最后通话时长（秒，未接通为0）
     */
    @Schema(description = "最后通话时长(秒,未接通为0)")
    @TableField("last_talk_duration")
    private Integer lastTalkDuration;
    
    
     
    /**
     *  拨打次数 
     */
    @Schema(description = "拨打次数")
    @TableField("attempt_count")
    private Integer attemptCount;
    
    
     
    /**
     *  计划呼叫时间(预约回访) 
     */
    @Schema(description = "计划呼叫时间(预约回访)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("scheduled_time")
    private Date scheduledTime;
    
    
     
    /**
     *  备注 
     */
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
    
    
    
    
    
    
    


}

