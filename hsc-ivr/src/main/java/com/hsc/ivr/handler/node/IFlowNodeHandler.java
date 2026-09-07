package com.hsc.ivr.handler.node;

import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.exception.FlowNodeException;
import org.springframework.statemachine.StateContext;

public interface IFlowNodeHandler {

    void handle(StateContext<Object, Object> stateContext);

    default void businessHandler(String event, FlowDataContext flowData) throws FlowNodeException{
        throw new FlowNodeException("businessHandler is not implemented");
    }
}
