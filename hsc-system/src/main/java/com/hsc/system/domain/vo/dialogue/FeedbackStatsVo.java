package com.hsc.system.domain.vo.dialogue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 摘要反馈满意率统计视图（统计分析报表用）。
 *
 * <p>维度 key：summary/keyword/role/keypoint/todo（call_ai_summary）+ abstract（过程摘要逐条），
 * 共 6 个；xmind 无前端反馈入口，不统计。满意率由前端按 good/(good+bad) 计算，已评 0 显示 —。
 */
@Schema
@Data
public class FeedbackStatsVo {

    @Schema(description = "维度汇总（全时间范围累计）")
    private List<DimTotal> items;

    @Schema(description = "按天趋势（日期升序）")
    private List<TrendPoint> trend;

    @Schema
    @Data
    public static class DimTotal {

        @Schema(description = "维度key")
        private String key;

        @Schema(description = "满意数")
        private long good;

        @Schema(description = "不满意数")
        private long bad;
    }

    @Schema
    @Data
    public static class TrendPoint {

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "各维度当日计数")
        private Map<String, DimTotal> dims;
    }
}
