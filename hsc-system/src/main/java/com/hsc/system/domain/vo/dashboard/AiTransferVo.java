package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI 转人工率趋势（aiTransfer 接口返回，按天）。
 */
@Schema
@Data
public class AiTransferVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "按天序列")
    private List<Point> points;

    @Data
    public static class Point implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "日期标签(08-22)")
        private String label;

        @Schema(description = "当日 AI 接听量(is_ai_first=1)")
        private Long aiAnswered;

        @Schema(description = "当日转人工量(transfer_human=1)")
        private Long transferred;

        @Schema(description = "当日转人工率(百分比,无 AI 接听时为 null)")
        private BigDecimal rate;
    }
}
