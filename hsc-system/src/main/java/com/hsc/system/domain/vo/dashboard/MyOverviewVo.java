package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作台「我的今日」（myOverview 接口返回）。
 * 坐席定位：当前登录 userId → sip_agent.user_id；hasAgent=false 表示无坐席身份，前端隐藏个人区块。
 * AI 转给我 = transfer_human=1 AND agent_id=我的坐席（呼入经 AI 接听后转给我接手）。
 */
@Schema
@Data
public class MyOverviewVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否有坐席身份(false=前端隐藏我的今日/我的待拨)")
    private Boolean hasAgent;

    @Schema(description = "今日通话量")
    private Long callCount;

    @Schema(description = "今日接通量")
    private Long answeredCount;

    @Schema(description = "今日通话总时长(秒)")
    private Long totalDurationSec;

    @Schema(description = "AI 转给我(通)")
    private Long aiTransferToMe;
}
