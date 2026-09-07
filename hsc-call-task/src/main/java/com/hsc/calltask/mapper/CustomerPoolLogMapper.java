package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.entity.CustomerPoolLog;
import com.hsc.calltask.domain.vo.CustomerTransferTimelineVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户流转日志表(CustomerPoolLog)表数据库访问层
 *
 * @author danmo
 * @since 2026-09-03
 */
@Mapper
public interface CustomerPoolLogMapper extends BaseMapper<CustomerPoolLog> {

    /**
     * 客户流转时间线：归属动作（customer_pool_log）与外呼明细（call_task_dial_log）
     * 按 customer_id UNION 时间倒序混排，名字经 join 装饰
     */
    List<CustomerTransferTimelineVo> getTransferTimeline(@Param("customerId") Long customerId);
}
