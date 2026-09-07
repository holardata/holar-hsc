// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.entity;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;


/**
 * 外呼任务表(CallTask)表实体类
 *
 * @author danmo
 * @since 2025-07-08 10:42:38
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("call_task")
public class CallTask extends BaseEntity implements Serializable {
  private static final long serialVersionUID = -29835182615264258L;
   
    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


     
    /**
     *  任务名称 
     */
    @Schema(description = "任务名称")
    @TableField("name")
    private String name;
    
    
     
    /**
     *  任务状态(0-未开始 1-进行中 2-暂停 3-结束)
     */
    @Schema(description = "任务状态(0-未开始 1-进行中 2-暂停 3-结束)")
    @TableField("status")
    private Integer status;
    
    
     
    /**
     *  任务优先级 
     */
    @Schema(description = "任务优先级")
    @TableField("priority")
    private Integer priority;
    
    
     
    /**
     *  任务开始时间 
     */
    @Schema(description = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @TableField("start_day")
    private Date startDay;
    
    
     
    /**
     *  任务结束时间 
     */
    @Schema(description = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @TableField("end_day")
    private Date endDay;
    
    
     
    /**
     *  每天开始时间
     */
    // 字段名第二个字母大写：Lombok getSTime → Jackson 默认属性名 "STime" 与 JSON 键 "sTime" 失配，须 @JsonProperty 显式对齐（读写双向）
    @Schema(description = "每天开始时间")
    @JsonProperty("sTime")
    @TableField("s_time")
    private String sTime;


    /**
     *  每天结束时间
     */
    // 同上：Jackson 默认属性名 "ETime" 与 "eTime" 失配
    @Schema(description = "每天结束时间")
    @JsonProperty("eTime")
    @TableField("e_time")
    private String eTime;
    
    
     
    /**
     *  周期时间 
     */
    @Schema(description = "周期时间")
    @TableField("work_cycle")
    private String workCycle;
    


    /**
     *  分配方式 1-轮流 2-空闲
     */
    @Schema(description = "分配方式 1-轮流 2-空闲")
    @TableField("assignment_type")
    private Integer assignmentType;



    /**
     *  执行坐席列表（为空全部坐席）
     */
    @Schema(description = "执行坐席列表（为空全部坐席）")
    @TableField("agent_list")
    private String agentList;
    
    
     
    /**
     *  外显号码池
     */
    @Schema(description = "外显号码池")
    @TableField("phone_pool_id")
    private Long phonePoolId;


    /**
     *  备注
     */
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
    
    
    
    
    
    
    


}

