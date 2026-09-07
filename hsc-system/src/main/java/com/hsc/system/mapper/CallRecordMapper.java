package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.query.call.CallRecordQuery;
import com.hsc.system.domain.vo.call.CallRecordVo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.hsc.system.domain.entity.CallRecord;

import java.util.List;

/**
 * 呼叫记录表(CallRecord)表数据库访问层
 *
 * @author danmo
 * @since 2024-11-21 11:22:55
 */
@Repository()
@Mapper
public interface CallRecordMapper extends BaseMapper<CallRecord> {

    List<CallRecordVo> getCallPageList(CallRecordQuery query);
}

