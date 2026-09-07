package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.AbstractRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 通话过程摘要(abstract_record)表数据库访问层
 */
@Repository()
@Mapper
public interface AbstractRecordMapper extends BaseMapper<AbstractRecord> {

    /**
     * 过程摘要逐条反馈按天聚合，供满意率统计。行 key：statDate/abstractGood/abstractBad。
     *
     * @param begin 开始日期 yyyy-MM-dd（可空=不限）
     * @param end   结束日期 yyyy-MM-dd（可空=不限，SQL 内加一天含当天）
     */
    List<Map<String, Object>> selectFeedbackTrend(@Param("begin") String begin, @Param("end") String end);
}
