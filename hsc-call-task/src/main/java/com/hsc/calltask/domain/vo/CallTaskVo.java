// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hsc.system.domain.vo.BaseVo;
import com.hsc.system.domain.vo.agent.SipSimpleAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 呼叫任务返回参数
 * @author danmo
 * @date 2025/06/19 10:19
 */
@Schema(description = "呼叫任务返回参数")
@EqualsAndHashCode(callSuper = true)
@Data
public class CallTaskVo extends BaseVo {

    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    private Long id;


    /**
     *  任务名称
     */
    @Schema(description = "任务名称")
    private String name;



    /**
     *  任务状态(0-未开始 1-进行中 2-暂停 3-结束)
     */
    @Schema(description = "任务状态(0-未开始 1-进行中 2-暂停 3-结束)")
    private Integer status;



    /**
     *  任务优先级
     */
    @Schema(description = "任务优先级")
    private Integer priority;



    /**
     *  任务开始时间
     */
    @Schema(description = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date startDay;



    /**
     *  任务结束时间
     */
    @Schema(description = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date endDay;



    /**
     *  每天开始时间
     */
    // 字段名第二个字母大写：Lombok getSTime → Jackson 默认属性名 "STime" 与 JSON 键 "sTime" 失配，须 @JsonProperty 显式对齐（详情回显）
    @JsonProperty("sTime")
    @Schema(description = "每天开始时间")
    private String sTime;



    /**
     *  每天结束时间
     */
    // 同上：Jackson 默认属性名 "ETime" 与 "eTime" 失配
    @JsonProperty("eTime")
    @Schema(description = "每天结束时间")
    private String eTime;



    /**
     *  周期时间
     */
    @Schema(description = "周期时间")
    private String workCycle;


    /**
     *  分配方式 1-轮流 2-空闲
     */
    @Schema(description = "分配方式 1-轮流 2-空闲")
    private Integer assignmentType;



    /**
     *  执行坐席列表（为空全部坐席）
     */
    @Schema(description = "执行坐席列表（为空全部坐席）")
    private List<SipSimpleAgent> agentList;

    /**
     *  外显号码池
     */
    @Schema(description = "外显号码池")
    private Long phonePoolId;

    @Schema(description = "外显号码池名称")
    private String phonePoolName;



    /**
     *  备注
     */
    @Schema(description = "备注")
    private String remark;

}
