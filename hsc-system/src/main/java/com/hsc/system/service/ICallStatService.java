package com.hsc.system.service;

import com.hsc.system.domain.query.DashboardQuery;
import com.hsc.system.domain.vo.dashboard.AgentRankVo;
import com.hsc.system.domain.vo.dashboard.AiTransferVo;
import com.hsc.system.domain.vo.dashboard.DistGroupVo;
import com.hsc.system.domain.vo.dashboard.MyOverviewVo;
import com.hsc.system.domain.vo.dashboard.MyRecentVo;
import com.hsc.system.domain.vo.dashboard.NameCountVo;
import com.hsc.system.domain.vo.dashboard.OverviewVo;
import com.hsc.system.domain.vo.dashboard.RegionVo;
import com.hsc.system.domain.vo.dashboard.TrendVo;

import java.util.List;

/**
 * 话单统计服务（dashboard-analytics-revamp，全实时聚合无 T+1）。
 * 口径见该变更 design D4；坐席定位=登录 userId → sip_agent.user_id。
 */
public interface ICallStatService {

    OverviewVo overview(DashboardQuery query);

    TrendVo trend(DashboardQuery query);

    List<RegionVo> region(DashboardQuery query);

    List<AgentRankVo> agentRank(DashboardQuery query);

    List<NameCountVo> answerType(DashboardQuery query);

    List<NameCountVo> unanswered(DashboardQuery query);

    List<NameCountVo> hangup(DashboardQuery query);

    DistGroupVo dist(DashboardQuery query);

    List<List<Long>> heatmap(DashboardQuery query);

    AiTransferVo aiTransfer(DashboardQuery query);

    MyOverviewVo myOverview(Long userId);

    List<MyRecentVo> myRecent(Long userId);
}
