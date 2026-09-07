package com.hsc.calltask.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 外呼任务进度（dashboard taskProgress 接口返回）。
 * 仅统计进行中(status=1)任务；接通数无 assignment 落库字段（拨打走 Redis TTL 映射回写话单），
 * 故只含总数/已拨/进度。
 */
@Schema
@Data
public class TaskProgressVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "联系人总数")
    private Long totalNum;

    @Schema(description = "已拨打数")
    private Long dialedNum;

    /**
     * 接通数（最近结果为接通的联系人计数）
     */
    private Long connectedNum;

    @Schema(description = "进度百分比(已拨/总数)")
    private BigDecimal progress;
}
