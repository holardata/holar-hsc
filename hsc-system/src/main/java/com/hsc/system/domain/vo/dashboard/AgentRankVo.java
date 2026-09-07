package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 坐席绩效排行项（agentRank 接口返回，TOP10）。
 */
@Schema
@Data
public class AgentRankVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "坐席ID(sip_agent.id)")
    private Long agentId;

    @Schema(description = "坐席名称")
    private String agentName;

    @Schema(description = "通话量")
    private Long callCount;

    @Schema(description = "接通量")
    private Long answeredCount;

    @Schema(description = "接通率(百分比)")
    private BigDecimal answerRate;

    @Schema(description = "平均处理时长(秒)")
    private Long avgDurationSec;
}
