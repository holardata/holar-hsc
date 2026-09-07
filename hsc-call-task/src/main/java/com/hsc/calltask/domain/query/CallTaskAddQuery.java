// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hsc.system.domain.vo.agent.SipSimpleAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 新增任务入参
 *
 * @author danmo
 * @date 2025/06/19 09:56
 */
@Schema(description = "新增任务入参")
@Data
public class CallTaskAddQuery {

    /**
     * 主键ID
     */

    @Schema(description = "主键ID", hidden = true)
    private Long id;


    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Schema(description = "任务名称")
    private String name;


    /**
     * 任务优先级
     */
    @Schema(description = "任务优先级")
    private Integer priority;


    /**
     * 任务开始时间
     */
    @NotNull(message = "任务开始时间不能为空")
    @Schema(description = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDay;


    /**
     * 任务结束时间
     */
    @Schema(description = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDay;


    /**
     * 每天开始时间
     */
    // 字段名第二个字母大写：Lombok 生成 getSTime，Jackson 默认推导属性名 "STime" 与 JSON 键 "sTime" 失配（值被静默丢弃），须 @JsonProperty 显式对齐
    @NotBlank(message = "每天开始时间不能为空")
    @JsonProperty("sTime")
    @Schema(description = "每天开始时间")
    private String sTime;


    /**
     * 每天结束时间
     */
    // 同上：Jackson 默认属性名 "ETime" 与 "eTime" 失配
    @NotBlank(message = "每天结束时间不能为空")
    @JsonProperty("eTime")
    @Schema(description = "每天结束时间")
    private String eTime;


    /**
     * 周期时间
     */
    @NotBlank(message = "周期时间不能为空")
    @Schema(description = "周期时间")
    private String workCycle;


    /**
     * 分配方式 1-轮流 2-空闲
     */
    @NotNull(message = "分配方式不能为空")
    @Schema(description = "分配方式 1-轮流 2-空闲")
    private Integer assignmentType;


    /**
     * 执行坐席列表（为空全部坐席）
     */
    @Schema(description = "执行坐席列表（为空全部坐席）")
    private List<SipSimpleAgent> agentList;


    /**
     * 外显号码池
     */
    @NotNull(message = "外显号码池不能为空")
    @Schema(description = "外显号码池")
    private Long phonePoolId;


    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}
