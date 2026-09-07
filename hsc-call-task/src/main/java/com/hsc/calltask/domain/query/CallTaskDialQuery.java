package com.hsc.calltask.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 任务联系人拨打入参
 *
 * @author danmo
 * @since 2026-08-27
 */
@Schema
@Data
public class CallTaskDialQuery {

    /**
     * 任务联系人记录ID
     */
    @Schema(description = "任务联系人记录ID")
    private Long assignmentId;
}
