package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallAiSummary;

/**
 * 通话AI摘要扩展(call_ai_summary)表服务接口
 */
public interface ICallAiSummaryService extends IBaseService<CallAiSummary> {

    /** 按 callId 查询摘要记录；不存在返回新对象(仅设 callId，由调用方 saveOrUpdate)。 */
    CallAiSummary getOrCreateByCallId(String callId);
}
