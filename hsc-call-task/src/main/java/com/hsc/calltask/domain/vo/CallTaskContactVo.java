// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.vo;


import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 呼叫任务客户
 *
 * @author danmo
 * @date 2025/7/8 15:46
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "呼叫任务客户")
@Data
public class CallTaskContactVo extends BaseVo {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "客户ID")
    private Long id;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "客户手机号")
    private String phone;

    @Schema(description = "客户来源 0-人群导入 1-文件导入 2-API导入")
    private Integer source;

    @Schema(description = "客户扩展信息")
    private JSONObject ext;

    @Schema(description = "分配状态 0-未分配 1-已分配")
    private Integer assignStatus;

    @Schema(description = "人群ID")
    private Long crowdId;

    @Schema(description = "人群名称")
    private String crowdName;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "分配坐席ID")
    private Long agentId;

    @Schema(description = "分配坐席名称")
    private String agentName;

    @Schema(description = "分配时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date assignTime;

    @Schema(description = "拨打状态 0-未拨打 1-已拨打")
    private Integer callStatus;
    /**
     * 最近拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败(NULL=未拨打或历史未知)
     */
    private Integer callResult;

    /**
     * 最后拨打完成时间
     */
    private Date lastDialTime;

    /**
     * 最后通话时长(秒,未接通为0)
     */
    private Integer lastTalkDuration;

    @Schema(description = "拨打次数")
    private Integer attemptCount;

    @Schema(description = "计划呼叫时间(预约回访)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledTime;

    @Schema(description = "备注")
    private String remark;

}
