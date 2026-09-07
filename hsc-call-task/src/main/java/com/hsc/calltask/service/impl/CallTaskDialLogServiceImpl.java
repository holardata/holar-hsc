package com.hsc.calltask.service.impl;

import com.hsc.calltask.domain.entity.CallTaskDialLog;
import com.hsc.calltask.mapper.CallTaskDialLogMapper;
import com.hsc.calltask.service.ICallTaskDialLogService;
import com.hsc.common.base.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 外呼拨打历史表(CallTaskDialLog)表服务实现类
 *
 * @author danmo
 * @since 2026-09-03
 */
@Service
public class CallTaskDialLogServiceImpl extends BaseServiceImpl<CallTaskDialLogMapper, CallTaskDialLog> implements ICallTaskDialLogService {

}
