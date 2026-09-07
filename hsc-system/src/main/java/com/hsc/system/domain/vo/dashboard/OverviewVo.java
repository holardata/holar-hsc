package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 指标卡总览（overview 接口返回）：6 张指标卡的当前周期值 + 上一等长周期值（环比）+ 周期内 sparkline 序列。
 * 口径见 dashboard-analytics-revamp design D4：接通=answer_flag=0；direction 1=呼出 2=呼入；
 * 放弃率=呼入未接通占比；AI 接听占比=is_ai_first/呼入；AI 转人工率=transfer_human/AI接听。
 */
@Schema
@Data
public class OverviewVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前周期指标")
    private MetricSet current;

    @Schema(description = "上一等长周期指标(环比基准)")
    private MetricSet previous;

    @Schema(description = "周期内 sparkline 序列(粒度点,前端每卡取对应字段)")
    private List<SparkPoint> spark;

    @Data
    public static class MetricSet implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "总呼叫量")
        private Long totalCall;

        @Schema(description = "呼入量(direction=2)")
        private Long inbound;

        @Schema(description = "呼出量(direction=1)")
        private Long outbound;

        @Schema(description = "接通量(answer_flag=0)")
        private Long answeredCount;

        @Schema(description = "接通率(百分比,0-100)")
        private BigDecimal answerRate;

        @Schema(description = "平均处理时长(秒,接通通话 call_end_time-answer_time 均值)")
        private Long avgDurationSec;

        @Schema(description = "呼入未接通量")
        private Long abandoned;

        @Schema(description = "放弃率(百分比,呼入未接通/呼入)")
        private BigDecimal abandonRate;

        @Schema(description = "AI接听量(is_ai_first=1)")
        private Long aiAnswered;

        @Schema(description = "AI接听占比(百分比,/呼入)")
        private BigDecimal aiAnswerRate;

        @Schema(description = "AI转人工量(transfer_human=1)")
        private Long transferCount;

        @Schema(description = "AI转人工率(百分比,/AI接听)")
        private BigDecimal transferRate;
    }

    @Data
    public static class SparkPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "时间点标签(小时'08:00'或日期'08-22')")
        private String label;

        @Schema(description = "该点总呼叫量")
        private Long totalCall;

        @Schema(description = "该点接通率(百分比)")
        private BigDecimal answerRate;

        @Schema(description = "该点平均处理时长(秒)")
        private Long avgDurationSec;

        @Schema(description = "该点放弃率(百分比)")
        private BigDecimal abandonRate;

        @Schema(description = "该点AI接听占比(百分比)")
        private BigDecimal aiAnswerRate;

        @Schema(description = "该点AI转人工率(百分比)")
        private BigDecimal transferRate;
    }
}
