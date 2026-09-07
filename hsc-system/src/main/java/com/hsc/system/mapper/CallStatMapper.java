package com.hsc.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 话单统计聚合（dashboard-analytics-revamp）。
 * 全部实时 SQL 聚合、无 T+1 中间表；口径见该变更 design D4：
 * 接通=answer_flag=0、direction 1=呼出 2=呼入、放弃=呼入未接通、
 * AI 接听=is_ai_first=1、转人工=transfer_human=1、归属地=number_location(merger_name 逗号切分)。
 * 时间条件统一为闭开区间：call_start_time >= #{begin} AND call_start_time < #{end}。
 */
@Mapper
@Repository
public interface CallStatMapper {

    /** 指标卡单段聚合（overview 当前/上期共用）：总量/呼入/呼出/接通/放弃/AI/转人工/均长 */
    Map<String, Object> selectOverview(@Param("begin") String begin, @Param("end") String end);

    /** 按粒度序列聚合（trend 与 overview.sparkline 共用），fmt=DATE_FORMAT 格式（'%H:00'/'%m-%d'） */
    List<Map<String, Object>> selectSeries(@Param("begin") String begin, @Param("end") String end, @Param("fmt") String fmt);

    /** 归属地省级聚合（merger_name 第 2 段=省全名，与地图 GeoJSON 天然对齐） */
    List<Map<String, Object>> selectRegionProvince(@Param("begin") String begin, @Param("end") String end);

    /** 归属地省内市级聚合（merger_name 第 3 段=市全名；province=省全名） */
    List<Map<String, Object>> selectRegionCity(@Param("begin") String begin, @Param("end") String end, @Param("province") String province);

    /** 坐席绩效 TOP10（排除 'AI智能坐席' 伪坐席） */
    List<Map<String, Object>> selectAgentRank(@Param("begin") String begin, @Param("end") String end);

    /** 接听方式原始聚合（接通呼入 join call_route，按 route_type+transfer_human 分组） */
    List<Map<String, Object>> selectAnswerType(@Param("begin") String begin, @Param("end") String end);

    /** 未接通原因原始聚合（answer_flag<>0，按 direction+answer_flag 分组，含振铃标记量） */
    List<Map<String, Object>> selectUnanswered(@Param("begin") String begin, @Param("end") String end);

    /** 挂机方向聚合（接通通话，hangup_dir 1/2/3 三个计数一行返回） */
    Map<String, Object> selectHangup(@Param("begin") String begin, @Param("end") String end);

    /** 分布分桶（时长 6 桶 d1-d6 + 接通耗时 6 桶 s1-s6 一行返回） */
    Map<String, Object> selectDist(@Param("begin") String begin, @Param("end") String end);

    /** 时段热力原始聚合（DAYOFWEEK 原值 + 小时 + 计数） */
    List<Map<String, Object>> selectHeatmap(@Param("begin") String begin, @Param("end") String end);

    /** AI 转人工按天聚合（AI 接听量 + 转人工量） */
    List<Map<String, Object>> selectAiTransfer(@Param("begin") String begin, @Param("end") String end);

    /** 我的今日聚合（agent 维度：量/接通/总时长/AI 转给我） */
    Map<String, Object> selectMyOverview(@Param("agentId") Long agentId, @Param("begin") String begin, @Param("end") String end);

    /** 我的最近通话快照（倒序 limit 条） */
    List<Map<String, Object>> selectMyRecent(@Param("agentId") Long agentId, @Param("limitNum") int limitNum);
}
