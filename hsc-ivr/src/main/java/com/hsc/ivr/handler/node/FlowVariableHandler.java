package com.hsc.ivr.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.SpelUtil;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowVariableNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 变量赋值节点处理（businessType=10）。
 *
 * <p>对节点配置的 {key, SpEL 表达式} 列表逐项求值写变量袋（一次可赋多个变量），
 * 完成后 notice(2,"next") 流转。借鉴 SmartCall GlobeValueGranter 的求值双写思路
 * （hsc 单容器变量袋只写一处）。
 *
 * @author pangshuai
 */
@Slf4j
@Component("FlowVariableHandler")
public class FlowVariableHandler extends AbstractIFlowNodeHandler {

    public FlowVariableHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
    }

    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            throw new FlowNodeException("节点配置错误");
        }
        FlowVariableNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowVariableNodeProperties.class);
        if (Objects.isNull(props) || Objects.isNull(props.getVariableItems())) {
            throw new FlowNodeException("变量节点未配置赋值项");
        }
        for (FlowVariableNodeProperties.VariableItem item : props.getVariableItems()) {
            if (StringUtils.isBlank(item.getKey())) {
                continue;
            }
            //值是 SpEL 表达式：纯字面量（如 vip）求值结果即原串；表达式（如 #StrUtil.substring(...)）求值取结果
            Object value = SpelUtil.parse(item.getVal(), flowData, Object.class);
            flowData.getVariables().put(item.getKey(), value == null ? item.getVal() : value);
        }
        iFlowNoticeService.notice(2, "next", flowData);
    }
}
