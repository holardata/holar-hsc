package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 话务量趋势（trend 接口返回）：呼入/呼出/放弃三序列，粒度自动（跨度≤1天按小时、>1天按天）。
 */
@Schema
@Data
public class TrendVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "粒度 hour=按小时 day=按天")
    private String granularity;

    @Schema(description = "趋势点序列")
    private List<Point> points;

    @Data
    public static class Point implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "时间点标签(小时'08:00'或日期'08-22')")
        private String label;

        @Schema(description = "呼入量")
        private Long inbound;

        @Schema(description = "呼出量")
        private Long outbound;

        @Schema(description = "放弃量(呼入未接通)")
        private Long abandoned;
    }
}
