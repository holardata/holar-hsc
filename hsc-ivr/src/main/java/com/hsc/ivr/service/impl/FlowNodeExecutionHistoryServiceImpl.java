package com.hsc.ivr.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.ivr.mapper.FlowNodeExecutionHistoryMapper;
import com.hsc.ivr.domain.entity.FlowNodeExecutionHistory;
import com.hsc.ivr.service.IFlowNodeExecutionHistoryService;
import org.springframework.stereotype.Service;

/**
 * 记录每次节点执行的历史记录(FlowNodeExecutionHistory)表服务实现类
 *
 * @author danmo
 * @since 2024-12-17 10:55:17
 */
@Service
public class FlowNodeExecutionHistoryServiceImpl extends BaseServiceImpl<FlowNodeExecutionHistoryMapper, FlowNodeExecutionHistory> implements IFlowNodeExecutionHistoryService {

}

