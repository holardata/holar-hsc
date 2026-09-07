package com.hsc.calltask.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 私海客户收回入参（回公海；改派=收回+再分配，日志记先后两条）
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema(description = "私海客户收回入参")
@Data
public class CustomerPoolReleaseQuery {

    @NotEmpty(message = "客户ID不能为空")
    @Schema(description = "客户ID列表(单个/批量)")
    private List<Long> customerIds;

    @Schema(description = "收回原因(记流转日志)")
    private String reason;
}
