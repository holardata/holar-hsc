package com.hsc.calltask.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务联系人导入结果统计：任务内重复跳过数、跨进行中任务冲突与私海归属提示明细
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "任务联系人导入结果统计")
@Data
public class CallTaskContactImportResultVo {

    @Schema(description = "成功导入条数")
    private Integer imported = 0;

    @Schema(description = "任务内重复跳过条数")
    private Integer skipped = 0;

    @Schema(description = "是否需要确认后重试（存在跨进行中任务重复或号码已归属坐席私海）")
    private Boolean conflict = false;

    @Schema(description = "冲突明细（跨进行中任务名/私海归属提示，供前端 confirm 展示）")
    private List<String> conflictDetails = new ArrayList<>();

    public void addConflictDetail(String detail) {
        this.conflict = true;
        this.conflictDetails.add(detail);
    }
}
