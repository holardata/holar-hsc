package com.hsc.ivr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.ivr.domain.entity.IvrSatisfactionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * IVR满意度评分记录(IvrSatisfactionRecord)表数据库访问层
 *
 * @author pangshuai
 * @since 2026-08-17
 */
@Repository
@Mapper
public interface IvrSatisfactionRecordMapper extends BaseMapper<IvrSatisfactionRecord> {

    /** 满意度按评分聚合（dashboard satisfaction：score>0 计入，闭开时间区间） */
    List<Map<String, Object>> statByScore(@Param("begin") String begin, @Param("end") String end);
}
