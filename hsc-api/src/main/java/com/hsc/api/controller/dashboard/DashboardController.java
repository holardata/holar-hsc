package com.hsc.api.controller.dashboard;

import com.hsc.calltask.service.ICallTaskService;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.ivr.service.IIvrSatisfactionRecordService;
import com.hsc.security.utils.SecurityUtils;
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
import com.hsc.system.service.ICallStatService;
import com.hsc.calltask.domain.vo.TaskProgressVo;
import com.hsc.system.domain.vo.dashboard.SatisfactionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计看板（dashboard-analytics-revamp）。
 *
 * <p>「总览」页 + 「话务报表」页 + 工作台个人区块的数据接口，全部实时 SQL 聚合（无 T+1）。
 *
 * <p>权限约定：全部接口<b>不加 @PreAuthorize</b>——dashboard 家族静态路由全员可见、
 * 话务报表菜单可见性即访问控制（沿用 DialogueController 免权限查询接口先例）。
 * 口径见变更 design D4；跨模块职责：call_record 统计在 hsc-system（CallStatService）、
 * 满意度在 hsc-ivr、任务进度在 hsc-call-task（依赖方向约束）。
 */
@Slf4j
@RestController
@RequestMapping("/system/v1/dashboard")
@Tag(name = "统计看板", description = "总览/话务报表/工作台个人数据接口(全实时聚合)")
public class DashboardController extends BaseController {

    @Autowired
    private ICallStatService callStatService;

    @Autowired
    private IIvrSatisfactionRecordService satisfactionRecordService;

    @Autowired
    private ICallTaskService callTaskService;

    @Log(title = "指标卡总览", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "指标卡×6(当前+环比+sparkline)", method = "POST")
    @PostMapping("/overview")
    public ResResult<OverviewVo> overview(@RequestBody DashboardQuery query) {
        return success(callStatService.overview(query));
    }

    @Log(title = "话务量趋势", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "呼入/呼出/放弃趋势(粒度自动:≤1天按小时/跨天按天)", method = "POST")
    @PostMapping("/trend")
    public ResResult<TrendVo> trend(@RequestBody DashboardQuery query) {
        return success(callStatService.trend(query));
    }

    @Log(title = "归属地分布", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "归属地两级统计(level=province全国省级/city+province省内市级)", method = "POST")
    @PostMapping("/region")
    public ResResult<List<RegionVo>> region(@RequestBody DashboardQuery query) {
        return success(callStatService.region(query));
    }

    @Log(title = "坐席绩效排行", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "坐席绩效TOP10(排除AI智能坐席)", method = "POST")
    @PostMapping("/agentRank")
    public ResResult<List<AgentRankVo>> agentRank(@RequestBody DashboardQuery query) {
        return success(callStatService.agentRank(query));
    }

    @Log(title = "接听方式构成", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "接通呼入按路由类型构成(软电话坐席/座机/AI独立/AI转人工/IVR)", method = "POST")
    @PostMapping("/answerType")
    public ResResult<List<NameCountVo>> answerType(@RequestBody DashboardQuery query) {
        return success(callStatService.answerType(query));
    }

    @Log(title = "未接通原因分布", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "未接通原因(answer_flag×方向,振铃细分)", method = "POST")
    @PostMapping("/unanswered")
    public ResResult<List<NameCountVo>> unanswered(@RequestBody DashboardQuery query) {
        return success(callStatService.unanswered(query));
    }

    @Log(title = "挂机方向分布", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "挂机方向(主叫/被叫/系统,仅接通通话)", method = "POST")
    @PostMapping("/hangup")
    public ResResult<List<NameCountVo>> hangup(@RequestBody DashboardQuery query) {
        return success(callStatService.hangup(query));
    }

    @Log(title = "满意度统计", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "满意度(均分+1-5星分布,score>0计入)", method = "POST")
    @PostMapping("/satisfaction")
    public ResResult<SatisfactionVo> satisfaction(@RequestBody DashboardQuery query) {
        String begin = query.getStartTime();
        String end = query.getEndTime();
        return success(satisfactionRecordService.getSatisfaction(
                begin == null ? "2000-01-01 00:00:00" : begin,
                end == null ? "2999-01-01 00:00:00" : end));
    }

    @Log(title = "时段热力", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "周几×24小时话务矩阵(周一=0)", method = "POST")
    @PostMapping("/heatmap")
    public ResResult<List<List<Long>>> heatmap(@RequestBody DashboardQuery query) {
        return success(callStatService.heatmap(query));
    }

    @Log(title = "分布分桶", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "通话时长+接通耗时两组分桶(各6桶)", method = "POST")
    @PostMapping("/dist")
    public ResResult<DistGroupVo> dist(@RequestBody DashboardQuery query) {
        return success(callStatService.dist(query));
    }

    @Log(title = "AI转人工趋势", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按天AI转人工率序列", method = "POST")
    @PostMapping("/aiTransfer")
    public ResResult<AiTransferVo> aiTransfer(@RequestBody DashboardQuery query) {
        return success(callStatService.aiTransfer(query));
    }

    @Log(title = "外呼任务进度", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "进行中任务进度(总数/已拨/百分比)", method = "POST")
    @PostMapping("/taskProgress")
    public ResResult<List<TaskProgressVo>> taskProgress(@RequestBody DashboardQuery query) {
        return success(callTaskService.getTaskProgress());
    }

    @Log(title = "我的今日", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "工作台我的今日×4(量/接通/时长/AI转给我,无坐席hasAgent=false)", method = "POST")
    @PostMapping("/myOverview")
    public ResResult<MyOverviewVo> myOverview(@RequestBody DashboardQuery query) {
        return success(callStatService.myOverview(SecurityUtils.getUserId()));
    }

    @Log(title = "我的最近通话", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "工作台最近通话6条快照", method = "POST")
    @PostMapping("/myRecent")
    public ResResult<List<MyRecentVo>> myRecent(@RequestBody DashboardQuery query) {
        return success(callStatService.myRecent(SecurityUtils.getUserId()));
    }
}
