package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.CallAiSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 通话AI摘要扩展(call_ai_summary)表数据库访问层
 */
@Repository()
@Mapper
public interface CallAiSummaryMapper extends BaseMapper<CallAiSummary> {

    /**
     * 摘要反馈按天聚合（SUM CASE + GROUP BY 日期），供满意率统计。
     * 行 key：statDate + {summary/keyword/role/keypoint/todo}Good/Bad（xmind 无前端反馈入口，不统计）。
     *
     * @param begin 开始日期 yyyy-MM-dd（可空=不限）
     * @param end   结束日期 yyyy-MM-dd（可空=不限，SQL 内加一天含当天）
     */
    List<Map<String, Object>> selectFeedbackTrend(@Param("begin") String begin, @Param("end") String end);
}
