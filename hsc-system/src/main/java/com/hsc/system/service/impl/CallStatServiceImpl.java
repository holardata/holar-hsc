package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.system.domain.entity.SipAgent;
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
import com.hsc.system.mapper.CallStatMapper;
import com.hsc.system.service.ICallStatService;
import com.hsc.system.service.ISipAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 话单统计实现：Mapper 出原始聚合量、比率与映射在 Service 统一计算。
 * 口径（design D4）：接通=answer_flag=0；direction 1=呼出 2=呼入；放弃率=呼入未接通/呼入；
 * AI 接听占比=is_ai_first/呼入；AI 转人工率=transfer_human/AI 接听；接听方式=接通呼入按
 * route_type 映射（1/4 软电话坐席、7 座机、8 按 transfer_human 拆、5/6 IVR 放音）。
 */
@Slf4j
@Service
public class CallStatServiceImpl implements ICallStatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 跨度 ≤26h 视为单日（今日/昨日），趋势按小时；否则按天 */
    private static final long HOUR_GRANULARITY_MAX_HOURS = 26;

    @Autowired
    private CallStatMapper callStatMapper;

    @Autowired
    private ISipAgentService sipAgentService;

    // ============================== 时间区间 ==============================

    private LocalDateTime[] parseRange(DashboardQuery query) {
        LocalDateTime begin;
        LocalDateTime end;
        if (Objects.nonNull(query.getStartTime()) && Objects.nonNull(query.getEndTime())) {
            begin = LocalDateTime.parse(query.getStartTime(), FMT);
            end = LocalDateTime.parse(query.getEndTime(), FMT);
        } else {
            // 默认今日
            begin = LocalDate.now().atStartOfDay();
            end = begin.plusDays(1);
        }
        return new LocalDateTime[]{begin, end};
    }

    private String fmt(LocalDateTime time) {
        return time.format(FMT);
    }

    // ============================== 指标卡 ==============================

    @Override
    public OverviewVo overview(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        Duration span = Duration.between(range[0], range[1]);
        LocalDateTime prevEnd = range[0];
        LocalDateTime prevBegin = prevEnd.minus(span);

        OverviewVo vo = new OverviewVo();
        vo.setCurrent(buildMetricSet(fmt(range[0]), fmt(range[1])));
        vo.setPrevious(buildMetricSet(fmt(prevBegin), fmt(prevEnd)));
        // sparkline：当前周期按粒度切点，前端每卡取对应字段
        List<Map<String, Object>> series = callStatMapper.selectSeries(fmt(range[0]), fmt(range[1]), granularityFmt(span));
        List<OverviewVo.SparkPoint> spark = new ArrayList<>();
        for (Map<String, Object> row : series) {
            OverviewVo.SparkPoint p = new OverviewVo.SparkPoint();
            p.setLabel(asStr(row.get("label")));
            long total = asLong(row.get("totalCall"));
            long inbound = asLong(row.get("inbound"));
            long answered = asLong(row.get("answeredCount"));
            long abandoned = asLong(row.get("abandoned"));
            long aiAnswered = asLong(row.get("aiAnswered"));
            long transferCount = asLong(row.get("transferCount"));
            p.setTotalCall(total);
            p.setAnswerRate(rate(answered, total));
            p.setAvgDurationSec(roundAvg(row.get("avgDurationSec")));
            p.setAbandonRate(rate(abandoned, inbound));
            p.setAiAnswerRate(rate(aiAnswered, inbound));
            p.setTransferRate(rate(transferCount, aiAnswered));
            spark.add(p);
        }
        vo.setSpark(spark);
        return vo;
    }

    private OverviewVo.MetricSet buildMetricSet(String begin, String end) {
        Map<String, Object> m = callStatMapper.selectOverview(begin, end);
        OverviewVo.MetricSet set = new OverviewVo.MetricSet();
        long total = asLong(m.get("totalCall"));
        long inbound = asLong(m.get("inbound"));
        long answered = asLong(m.get("answeredCount"));
        long abandoned = asLong(m.get("abandoned"));
        long aiAnswered = asLong(m.get("aiAnswered"));
        long transferCount = asLong(m.get("transferCount"));
        set.setTotalCall(total);
        set.setInbound(inbound);
        set.setOutbound(asLong(m.get("outbound")));
        set.setAnsweredCount(answered);
        set.setAnswerRate(rate(answered, total));
        set.setAvgDurationSec(roundAvg(m.get("avgDurationSec")));
        set.setAbandoned(abandoned);
        set.setAbandonRate(rate(abandoned, inbound));
        set.setAiAnswered(aiAnswered);
        set.setAiAnswerRate(rate(aiAnswered, inbound));
        set.setTransferCount(transferCount);
        set.setTransferRate(rate(transferCount, aiAnswered));
        return set;
    }

    // ============================== 趋势 ==============================

    @Override
    public TrendVo trend(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        Duration span = Duration.between(range[0], range[1]);
        String granularity = span.toHours() <= HOUR_GRANULARITY_MAX_HOURS ? "hour" : "day";
        List<Map<String, Object>> series = callStatMapper.selectSeries(fmt(range[0]), fmt(range[1]), granularityFmt(span));

        TrendVo vo = new TrendVo();
        vo.setGranularity(granularity);
        List<TrendVo.Point> points = new ArrayList<>();
        for (Map<String, Object> row : series) {
            TrendVo.Point p = new TrendVo.Point();
            p.setLabel(asStr(row.get("label")));
            p.setInbound(asLong(row.get("inbound")));
            p.setOutbound(asLong(row.get("outbound")));
            p.setAbandoned(asLong(row.get("abandoned")));
            points.add(p);
        }
        vo.setPoints(points);
        return vo;
    }

    private String granularityFmt(Duration span) {
        return span.toHours() <= HOUR_GRANULARITY_MAX_HOURS ? "%H:00" : "%m-%d";
    }

    // ============================== 归属地 ==============================

    @Override
    public List<RegionVo> region(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<RegionVo> result = new ArrayList<>();
        if ("city".equals(query.getLevel())) {
            // 前端下钻传省全名（与 GeoJSON 一致），merger_name 第 2 段同为全名，直接匹配
            List<Map<String, Object>> rows = callStatMapper.selectRegionCity(fmt(range[0]), fmt(range[1]), query.getProvince());
            for (Map<String, Object> row : rows) {
                // 市段已带"市"字（merger_name），基本天然对齐 GeoJSON；未知市名原样返回不入图的由前端兜底
                result.add(new RegionVo(asStr(row.get("city")), asLong(row.get("inbound")), asLong(row.get("outbound"))));
            }
            return result;
        }
        List<Map<String, Object>> rows = callStatMapper.selectRegionProvince(fmt(range[0]), fmt(range[1]));
        for (Map<String, Object> row : rows) {
            // merger_name 第 2 段即省全名（如"陕西省"），与地图 GeoJSON 天然对齐，原样返回
            result.add(new RegionVo(asStr(row.get("province")),
                    asLong(row.get("inbound")), asLong(row.get("outbound"))));
        }
        return result;
    }

    // ============================== 坐席排行 ==============================

    @Override
    public List<AgentRankVo> agentRank(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<Map<String, Object>> rows = callStatMapper.selectAgentRank(fmt(range[0]), fmt(range[1]));
        List<AgentRankVo> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AgentRankVo vo = new AgentRankVo();
            vo.setAgentId(asLong(row.get("agentId")));
            vo.setAgentName(asStr(row.get("agentName")));
            long callCount = asLong(row.get("callCount"));
            vo.setCallCount(callCount);
            vo.setAnsweredCount(asLong(row.get("answeredCount")));
            vo.setAnswerRate(rate(asLong(row.get("answeredCount")), callCount));
            vo.setAvgDurationSec(roundAvg(row.get("avgDurationSec")));
            result.add(vo);
        }
        return result;
    }

    // ============================== 接听方式 ==============================

    @Override
    public List<NameCountVo> answerType(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<Map<String, Object>> rows = callStatMapper.selectAnswerType(fmt(range[0]), fmt(range[1]));
        Map<String, Long> merged = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer routeType = asInt(row.get("routeType"));
            Integer transferHuman = asInt(row.get("transferHuman"));
            merged.merge(answerTypeName(routeType, transferHuman), asLong(row.get("cnt")), Long::sum);
        }
        return toDescList(merged);
    }

    private String answerTypeName(Integer routeType, Integer transferHuman) {
        if (Objects.equals(routeType, 1) || Objects.equals(routeType, 4)) {
            return "软电话坐席";
        }
        if (Objects.equals(routeType, 7)) {
            return "座机";
        }
        if (Objects.equals(routeType, 8)) {
            return Objects.equals(transferHuman, 1) ? "AI → 转人工" : "AI 独立完成";
        }
        if (Objects.equals(routeType, 5) || Objects.equals(routeType, 6)) {
            return "IVR/放音等";
        }
        return "其他";
    }

    // ============================== 未接原因 / 挂机方向 ==============================

    @Override
    public List<NameCountVo> unanswered(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<Map<String, Object>> rows = callStatMapper.selectUnanswered(fmt(range[0]), fmt(range[1]));
        Map<String, Long> merged = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Integer direction = asInt(row.get("direction"));
            long ringing = asLong(row.get("ringingCount"));
            long cnt = asLong(row.get("cnt"));
            if (Objects.equals(direction, 1)) {
                // 呼出未接通=客户未接（含坐席接通客户未接的 answer_flag=2 场景）
                merged.merge("呼出·客户未接", cnt, Long::sum);
            } else {
                // 呼入未接通：有振铃记录=振铃后放弃，否则坐席未接听
                merged.merge("呼入·振铃后放弃", ringing, Long::sum);
                merged.merge("呼入·坐席未接听", cnt - ringing, Long::sum);
            }
        }
        return toDescList(merged);
    }

    @Override
    public List<NameCountVo> hangup(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        Map<String, Object> m = emptyIfNull(callStatMapper.selectHangup(fmt(range[0]), fmt(range[1])));
        List<NameCountVo> result = new ArrayList<>();
        result.add(new NameCountVo("主叫挂机（客户先挂）", asLong(m.get("callerHangup"))));
        result.add(new NameCountVo("被叫挂机", asLong(m.get("calleeHangup"))));
        result.add(new NameCountVo("系统挂机", asLong(m.get("systemHangup"))));
        return result;
    }

    // ============================== 分布分桶 ==============================

    @Override
    public DistGroupVo dist(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        Map<String, Object> m = emptyIfNull(callStatMapper.selectDist(fmt(range[0]), fmt(range[1])));
        DistGroupVo vo = new DistGroupVo();
        vo.setDurationBuckets(Arrays.asList(
                new NameCountVo("<30s", asLong(m.get("d1"))),
                new NameCountVo("30s-1m", asLong(m.get("d2"))),
                new NameCountVo("1-3m", asLong(m.get("d3"))),
                new NameCountVo("3-5m", asLong(m.get("d4"))),
                new NameCountVo("5-10m", asLong(m.get("d5"))),
                new NameCountVo(">10m", asLong(m.get("d6")))));
        vo.setSpeedBuckets(Arrays.asList(
                new NameCountVo("<5s", asLong(m.get("s1"))),
                new NameCountVo("5-10s", asLong(m.get("s2"))),
                new NameCountVo("10-20s", asLong(m.get("s3"))),
                new NameCountVo("20-30s", asLong(m.get("s4"))),
                new NameCountVo("30-60s", asLong(m.get("s5"))),
                new NameCountVo(">60s", asLong(m.get("s6")))));
        return vo;
    }

    // ============================== 热力图 ==============================

    @Override
    public List<List<Long>> heatmap(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<Map<String, Object>> rows = callStatMapper.selectHeatmap(fmt(range[0]), fmt(range[1]));
        // 7 行(周一=0..周日=6) × 24 列，前端 echarts 直接消费
        List<List<Long>> matrix = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            List<Long> row = new ArrayList<>(24);
            for (int h = 0; h < 24; h++) {
                row.add(0L);
            }
            matrix.add(row);
        }
        for (Map<String, Object> row : rows) {
            int weekDay = asInt(row.get("weekDay"));   // MySQL DAYOFWEEK：周日=1..周六=7
            int javaDay = (weekDay + 5) % 7;           // 转周一=0..周日=6
            int hour = asInt(row.get("hourOfDay"));
            matrix.get(javaDay).set(hour, asLong(row.get("cnt")));
        }
        return matrix;
    }

    // ============================== AI 转人工趋势 ==============================

    @Override
    public AiTransferVo aiTransfer(DashboardQuery query) {
        LocalDateTime[] range = parseRange(query);
        List<Map<String, Object>> rows = callStatMapper.selectAiTransfer(fmt(range[0]), fmt(range[1]));
        AiTransferVo vo = new AiTransferVo();
        List<AiTransferVo.Point> points = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AiTransferVo.Point p = new AiTransferVo.Point();
            p.setLabel(asStr(row.get("label")));
            long aiAnswered = asLong(row.get("aiAnswered"));
            p.setAiAnswered(aiAnswered);
            p.setTransferred(asLong(row.get("transferred")));
            p.setRate(rate(asLong(row.get("transferred")), aiAnswered));
            points.add(p);
        }
        vo.setPoints(points);
        return vo;
    }

    // ============================== 我的（工作台） ==============================

    @Override
    public MyOverviewVo myOverview(Long userId) {
        MyOverviewVo vo = new MyOverviewVo();
        vo.setHasAgent(false);
        vo.setCallCount(0L);
        vo.setAnsweredCount(0L);
        vo.setTotalDurationSec(0L);
        vo.setAiTransferToMe(0L);
        Long agentId = findAgentId(userId);
        if (Objects.isNull(agentId)) {
            return vo;
        }
        vo.setHasAgent(true);
        LocalDateTime begin = LocalDate.now().atStartOfDay();
        Map<String, Object> m = emptyIfNull(callStatMapper.selectMyOverview(agentId, fmt(begin), fmt(begin.plusDays(1))));
        vo.setCallCount(asLong(m.get("callCount")));
        vo.setAnsweredCount(asLong(m.get("answeredCount")));
        vo.setTotalDurationSec(asLong(m.get("totalDurationSec")));
        vo.setAiTransferToMe(asLong(m.get("aiTransferToMe")));
        return vo;
    }

    @Override
    public List<MyRecentVo> myRecent(Long userId) {
        Long agentId = findAgentId(userId);
        if (Objects.isNull(agentId)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = callStatMapper.selectMyRecent(agentId, 6);
        List<MyRecentVo> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MyRecentVo vo = new MyRecentVo();
            vo.setId(asLong(row.get("id")));
            vo.setCallId(asStr(row.get("callId")));
            Integer direction = asInt(row.get("direction"));
            vo.setDirection(direction);
            vo.setAnswered(Objects.equals(asInt(row.get("answerFlag")), 0));
            // 对端号码：呼入=主叫、呼出=被叫
            vo.setPhone(Objects.equals(direction, 2) ? asStr(row.get("callerNumber")) : asStr(row.get("calleeNumber")));
            vo.setAiTransfer(Objects.equals(asInt(row.get("transferHuman")), 1));
            vo.setDurationSec(asLong(row.get("durationSec")));
            Object startTime = row.get("startTime");
            vo.setStartTime(asDate(startTime));
            result.add(vo);
        }
        return result;
    }

    private Long findAgentId(Long userId) {
        if (Objects.isNull(userId)) {
            return null;
        }
        SipAgent agent = sipAgentService.getOne(new LambdaQueryWrapper<SipAgent>()
                .eq(SipAgent::getUserId, userId).last("limit 1"));
        return Objects.isNull(agent) ? null : agent.getId();
    }

    // ============================== 工具 ==============================

    /**
     * 单行聚合查询空库兜底：无 GROUP BY 的纯聚合（selectHangup/selectDist 全列 SUM）在空库时
     * 各列全 NULL，会被 MyBatis 映射为 null（returnInstanceForEmptyRow 默认 false），统一归一空 Map，
     * 使下游 asLong/roundAvg 按 0 展示。selectOverview/selectMyOverview 带 COUNT(*) 恒有非空行，套用仅为统一。
     */
    private Map<String, Object> emptyIfNull(Map<String, Object> m) {
        return Objects.isNull(m) ? new HashMap<>() : m;
    }

    private List<NameCountVo> toDescList(Map<String, Long> merged) {
        List<NameCountVo> list = new ArrayList<>();
        merged.forEach((k, v) -> list.add(new NameCountVo(k, v)));
        list.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return list;
    }

    /** 百分比（0-100，两位小数）；分母为 0 返回 null（前端显示 --） */
    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return new BigDecimal(numerator * 100L).divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
    }

    private Long roundAvg(Object avg) {
        if (Objects.isNull(avg)) {
            return 0L;
        }
        return new BigDecimal(String.valueOf(avg)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private long asLong(Object v) {
        return Objects.isNull(v) ? 0L : ((Number) v).longValue();
    }

    private int asInt(Object v) {
        return Objects.isNull(v) ? 0 : ((Number) v).intValue();
    }

    private String asStr(Object v) {
        return Objects.isNull(v) ? "" : String.valueOf(v);
    }

    /**
     * Map 取 DATETIME 列转 Date：MySQL 8 驱动 getObject 对 DATETIME 返回 LocalDateTime
     * （非 Date），直接强转会 ClassCastException；Date/Timestamp（含子类）原样返回，兼容驱动行为差异。
     */
    private Date asDate(Object v) {
        if (Objects.isNull(v)) {
            return null;
        }
        if (v instanceof Date date) {
            return date;
        }
        if (v instanceof LocalDateTime time) {
            return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }
}
