package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.entity.CallTaskDialLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 外呼拨打历史表(CallTaskDialLog)表数据库访问层
 *
 * @author danmo
 * @since 2026-09-03
 */
@Mapper
public interface CallTaskDialLogMapper extends BaseMapper<CallTaskDialLog> {

}
