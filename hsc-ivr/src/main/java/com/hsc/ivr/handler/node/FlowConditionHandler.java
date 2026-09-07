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
import com.hsc.ivr.properties.FlowConditionNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 条件判断节点处理（businessType=9）。
 *
 * <p>分支列表按 order 排序、SpEL 求值命中即停（求值异常视为不命中），
 * 命中后抛 next_{branchId} 流转事件走对应出口边；无分支命中走最后一个 else 兜底分支。
 * 借鉴 SmartCall ConditionGranter 的遍历/兜底/命中break结构（借逻辑不借框架）。
 *
 * @author pangshuai
 */
@Slf4j
@Component("FlowConditionHandler")
public class FlowConditionHandler extends AbstractIFlowNodeHandler {

    /** 兜底分支的条件关键字（不参与求值，直接命中） */
    private static final String ELSE_KEYWORD = "else";

    public FlowConditionHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
    }

    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            throw new FlowNodeException("节点配置错误");
        }
        FlowConditionNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowConditionNodeProperties.class);
        if (Objects.isNull(props) || Objects.isNull(props.getConditions()) || props.getConditions().isEmpty()) {
            throw new FlowNodeException("条件节点未配置分支");
        }
        List<FlowConditionNodeProperties.ConditionBranch> branches = props.getConditions().stream()
                .sorted(Comparator.comparing((FlowConditionNodeProperties.ConditionBranch c) -> c.getOrder() == null ? 0 : c.getOrder()))
                .toList();
        //兜底 = 排序后最后一个分支（无论其 condition 是否写 else）
        FlowConditionNodeProperties.ConditionBranch fallback = branches.get(branches.size() - 1);
        for (FlowConditionNodeProperties.ConditionBranch branch : branches) {
            if (StringUtils.isBlank(branch.getCondition()) || ELSE_KEYWORD.equalsIgnoreCase(branch.getCondition().trim())) {
                continue;
            }
            //求值异常视为不命中（SpelUtil.parseBoolean 内部 catch 返回 false）
            if (SpelUtil.parseBoolean(branch.getCondition(), flowData)) {
                iFlowNoticeService.notice(2, "next_" + branch.getBranchId(), flowData);
                return;
            }
        }
        iFlowNoticeService.notice(2, "next_" + fallback.getBranchId(), flowData);
    }
}
