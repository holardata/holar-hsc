// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.query.call.CallRecordQuery;
import com.hsc.system.domain.vo.call.CallRecordVo;
import com.hsc.system.mapper.CallRecordMapper;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.service.ICallRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 呼叫记录表(CallRecord)表服务实现类
 *
 * @author danmo
 * @since 2024-11-21 11:23:02
 */
@Service
public class CallRecordServiceImpl extends BaseServiceImpl<CallRecordMapper, CallRecord> implements ICallRecordService {

    @Override
    public List<CallRecordVo> getCallPageList(CallRecordQuery query)
    {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return baseMapper.getCallPageList(query);
    }

    @Override
    public CallRecord getByCallId(String callId) {
        if (callId == null || callId.isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<CallRecord>()
                .eq(CallRecord::getCallId, callId)
                .last("limit 1"));
    }
}

