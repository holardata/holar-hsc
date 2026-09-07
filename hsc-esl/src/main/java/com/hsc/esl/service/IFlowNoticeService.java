package com.hsc.esl.service;

import com.hsc.common.constant.FlowDataContext;

public interface IFlowNoticeService {

    void notice(String address, Long callId, String uniqueId, Long flowId);

    void notice(Integer type, String event, FlowDataContext data);
}
