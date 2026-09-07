package com.hsc.calltask.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 坐席任务汇总（工作台「任务」tab 一级列表）。
 * 仅统计进行中(status=1)任务；undialed=0 表示本人名单已清完（前端置底弱化显示）。
 */
@Schema
@Data
public class CallTaskMySummaryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "本人分配总数")
    private Long total;

    @Schema(description = "本人未拨打数")
    private Long undialed;
}
