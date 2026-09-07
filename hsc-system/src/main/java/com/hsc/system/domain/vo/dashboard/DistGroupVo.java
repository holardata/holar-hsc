package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分布分桶组（dist 接口返回）：通话时长分桶 + 接通耗时分桶两组一次返回。
 * 时长桶（接通通话 call_end_time-answer_time）：<30s / 30s-1m / 1-3m / 3-5m / 5-10m / >10m
 * 耗时桶（bridge_time-call_start_time）：<5s / 5-10s / 10-20s / 20-30s / 30-60s / >60s
 */
@Schema
@Data
public class DistGroupVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "通话时长分布(6桶)")
    private List<NameCountVo> durationBuckets;

    @Schema(description = "接通耗时分布(6桶)")
    private List<NameCountVo> speedBuckets;
}
