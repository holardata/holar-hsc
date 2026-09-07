package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.mapper.CallAiSummaryMapper;
import com.hsc.system.service.ICallAiSummaryService;
import org.springframework.stereotype.Service;

/**
 * 通话AI摘要扩展(call_ai_summary)表服务实现
 */
@Service
public class CallAiSummaryServiceImpl extends BaseServiceImpl<CallAiSummaryMapper, CallAiSummary> implements ICallAiSummaryService {

    @Override
    public CallAiSummary getOrCreateByCallId(String callId) {
        CallAiSummary summary = getOne(new LambdaQueryWrapper<CallAiSummary>()
                .eq(CallAiSummary::getCallId, callId)
                .last("limit 1"));
        if (summary == null) {
            summary = new CallAiSummary();
            summary.setCallId(callId);
        }
        return summary;
    }
}
