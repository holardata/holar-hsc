// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.enums.FlowNodeTypeEnum;
import com.hsc.common.utils.SpringUtils;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.event.FlowEvent;
import com.hsc.ivr.domain.entity.FlowInstances;
import com.hsc.ivr.domain.vo.FlowEdgeVo;
import com.hsc.ivr.domain.vo.FlowInfoVo;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.handler.node.AbstractIFlowNodeHandler;
import com.hsc.ivr.properties.FlowNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.ivr.service.IvrDialogLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Component
public class FlowEventListener implements ApplicationListener<FlowEvent> {

    /**
     * 流程 node/edge 缓存 TTL（小时）：无主动清理点（编辑/发布/删除都不删缓存，靠 key 带版本隔离），
     * 旧版本缓存等 TTL 过期。须远大于任何一通电话的持续时长（长通话/排队场景），24h 无维护成本。
     */
    private static final int FLOW_CACHE_TTL_HOURS = 24;

    private final RedisStateMachinePersister<Object, Object> persister;
    private final IFlowInfoService iFlowInfoService;
    private final IFlowInstancesService iFlowInstancesService;
    private final RedisService redisService;
    private final IvrDialogLogger ivrDialogLogger;
    private final FsClient fsClient;

    @Override
    public void onApplicationEvent(FlowEvent event) {
        Integer eventType = event.getType();
        switch (eventType) {
            case 1 -> startStateMachine(event);
            case 2 -> transferStateMachine(event);
            case 3 -> endStateMachine(event);
            case 4 -> flowBusinessHandler(event);
            default -> log.info("未知事件类型 event:{}", event);
        }
    }


    /**
     * 结束
     *
     * @param event
     */
    private void endStateMachine(FlowEvent event) {
        log.info("endStateMachine event:{}", event);
        FlowDataContext flowData = event.getData();
        StateMachine<Object, Object> stateMachine = buildStateMachine(flowData);
        if (Objects.isNull(stateMachine)) {
            log.info("创建状态机失败 event:{}", event);
            return;
        }
        try {
            StateMachine<Object, Object> restore = persister.restore(stateMachine, StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, flowData.getInstanceId()));
            restore.stopReactively().subscribe();
        } catch (Exception e) {
            log.error("endStateMachine 恢复状态机异常:event:{},error:{}", event, e.getMessage(), e);
        }
        redisService.deleteObject(StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, event.getData().getInstanceId()));
    }

    /**
     * 转发
     *
     * @param event
     */
    private void transferStateMachine(FlowEvent event) {
        log.info("transferStateMachine event:{}", event);
        FlowDataContext flowData = event.getData();
        StateMachine<Object, Object> stateMachine = buildStateMachine(flowData);
        if (Objects.isNull(stateMachine)) {
            log.info("创建状态机失败 event:{}", event);
            return;
        }
        try {
            StateMachine<Object, Object> restore = persister.restore(stateMachine, StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, flowData.getInstanceId()));
            restore.getExtendedState().getVariables().put("flowData", event.getData());
            restore.sendEvent(event.getEvent());
        } catch (Exception e) {
            log.error("transfer 恢复状态机异常:event:{},error:{}", event, e.getMessage(), e);
        }
    }

    /**
     * 开始
     *
     * @param event
     */
    private void startStateMachine(FlowEvent event) {
        log.info("startStateMachine event:{}", event);
        FlowDataContext flowData = event.getData();
        FlowInfoVo info = iFlowInfoService.getInfo(flowData.getFlowId());
        if (Objects.isNull(info) || StringUtils.isBlank(info.getPublishedFlowData())) {
            // 流程不存在/未发布(脏配置，如溢出/路由指向已删流程)：原仅记日志返回会让主叫腿
            // 悬空听静音，统一挂断收尾（挂断事件照常触发话单收尾）
            log.warn("未找到已发布流程信息,挂断主叫腿 callId:{}, flowId:{}", flowData.getCallId(), flowData.getFlowId());
            fsClient.hangupCall(flowData.getAddress(), flowData.getCallId(), flowData.getUniqueId());
            return;
        }
        // 通话钉死发布版本：快照+版本号存进 flowData，后续流转/节点配置读取全部基于快照，
        // 不再回库——编辑草稿、再发布新版本都不影响本通电话（ivr-flow-version）
        flowData.setFlowJson(info.getPublishedFlowData());
        flowData.setVersion(info.getVersion() == null ? 0 : info.getVersion());
        StateMachine<Object, Object> stateMachine = buildStateMachine(flowData);
        if (Objects.isNull(stateMachine)) {
            log.info("创建状态机失败 event:{}", event);
            return;
        }
        //创建流程实例
        FlowInstances instance = FlowInstances.builder()
                .flowId(flowData.getFlowId())
                .callId(flowData.getCallId())
                .status(1)
                .startTime(new Date())
                .build();
        iFlowInstancesService.save(instance);
        flowData.setInstanceId(instance.getId());
        //启动状态机
        stateMachine.getExtendedState().getVariables().put("flowData", event.getData());
        stateMachine.startReactively().subscribe();
        try {
            persister.persist(stateMachine, StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, instance.getId()));
        } catch (Exception e) {
            log.error("持久化状态机异常:event:{},error:{}", event, e.getMessage(), e);
        }

    }


    /**
     * 从通话钉死的发布版快照（flowData.flowJson）构建状态机；node/edge 解析缓存 key 带发布版本号，
     * 同版本并发通话共享、跨版本天然隔离，TTL 兜底过期（无主动清理点——编辑/发布/删除都不删缓存）
     */
    private StateMachine<Object, Object> buildStateMachine(FlowDataContext flowData) {
        log.info("创建状态机：flowId:{}, version:{}", flowData.getFlowId(), flowData.getVersion());
        try {
            String flowJson = flowData.getFlowJson();
            if (StringUtils.isBlank(flowJson)) {
                // 升级窗口期的历史通话无快照（正常发布后新通话必有），无法继续流转
                log.info("通话缺少发布版流程快照 callId:{}, instanceId:{}", flowData.getCallId(), flowData.getInstanceId());
                return null;
            }
            JSONObject flowDataJson = JSONObject.parseObject(flowJson);
            List<FlowNodeVo> nodes =flowDataJson.getObject("nodes", new TypeReference<List<FlowNodeVo>>(){});

            String nodeKey = StringUtils.format(CacheConstants.CALL_IVR_FLOW_INFO_NODE_KEY, flowData.getFlowId(), flowData.getVersion());
            if(!redisService.keyIsExists(nodeKey)){
                Map<String, FlowNodeVo> flowNodeMap = nodes.stream().collect(Collectors.toMap(FlowNodeVo::getId, n -> n, (key1, key2) -> key1));
                redisService.setCacheMap(nodeKey, flowNodeMap);
                redisService.expire(nodeKey, FLOW_CACHE_TTL_HOURS, TimeUnit.HOURS);
            }

            //解析流程边（后续状态机边配置与出边缓存共用）
            List<FlowEdgeVo> edges = flowDataJson.getObject("edges", new TypeReference<List<FlowEdgeVo>>(){});
            // 边缓存（source→出边列表）：节点 handler 查 fail 等出边用（与节点缓存同生命周期）
            String edgeKey = StringUtils.format(CacheConstants.CALL_IVR_FLOW_INFO_EDGE_KEY, flowData.getFlowId(), flowData.getVersion());
            if(!redisService.keyIsExists(edgeKey)){
                Map<String, List<FlowEdgeVo>> edgeMap = edges.stream()
                        .collect(Collectors.groupingBy(FlowEdgeVo::getSourceNodeId));
                redisService.setCacheMap(edgeKey, edgeMap);
                redisService.expire(edgeKey, FLOW_CACHE_TTL_HOURS, TimeUnit.HOURS);
            }

            FlowNodeVo startNode = nodes.stream().filter(n -> Objects.equals("start", n.getId())).findFirst().orElseGet(null);
            if (Objects.isNull(startNode)) {
                log.info("未找到流程开始节点 id:{}", flowData.getFlowId());
                return null;
            }
            FlowNodeVo endNode = nodes.stream().filter(n -> Objects.equals("end", n.getId())).findFirst().orElseGet(null);
            if (Objects.isNull(endNode)) {
                log.info("未找到流程结束节点 id:{}", flowData.getFlowId());
                return null;
            }
            //配置状态机节点
            StateMachineBuilder.Builder<Object, Object> stateMachineBuilder = StateMachineBuilder.builder();
            stateMachineBuilder.configureConfiguration()
                    .withConfiguration()
                    .listener(new FlowStateMachineListener());
            stateMachineBuilder.configureStates().withStates()
                    .initial(startNode.getId()).states(nodes.stream().map(FlowNodeVo::getId).collect(Collectors.toSet()))
                    .end(endNode.getId());

            //配置状态机边
            StateMachineTransitionConfigurer<Object, Object> transitionConfigurer = stateMachineBuilder.configureTransitions();
            //end 兜底转移：每个 source 节点只注册一条 →end on "end"（节点 notice(2,"end") 结束流程用）；
            //原实现每条边都注册，同 source 多出边时会重复注册同 (source,event) 转移
            Set<String> endRegistered = new HashSet<>();
            for (FlowEdgeVo edge : edges) {
                String source = edge.getSourceNodeId();
                if (endRegistered.add(source)) {
                    transitionConfigurer.withExternal().source(source).target(endNode.getId()).event("end").and();
                }
                transitionConfigurer.withExternal().source(source).target(edge.getTargetNodeId()).event(edge.getEvent()).and();
            }
            return stateMachineBuilder.build();
        } catch (Exception e) {
            log.error("创建状态机异常 id:{},error:{}", flowData.getFlowId(), e.getMessage(), e);
            return null;
        }
    }

    public void flowBusinessHandler(FlowEvent event){
        FlowDataContext flowData = event.getData();
        // IVR 交互转写：DTMF 回调统一入口，先落按键记录（【按键】x/【未按键】）再路由节点业务
        ivrDialogLogger.logDtmf(flowData, event.getEvent());
        String currentNodeId = flowData.getCurrentNodeId();
        FlowNodeVo currentFlowNode = redisService.getCacheMapValue(StringUtils.format(CacheConstants.CALL_IVR_FLOW_INFO_NODE_KEY, flowData.getFlowId(), flowData.getVersion()), currentNodeId);
        if (Objects.isNull(currentFlowNode)) {
            log.info("未找到当前节点flowData:{}, id:{}", JSON.toJSONString(flowData), currentNodeId);
            return;
        }
        FlowNodeProperties flowNodeProperties = JSONObject.parseObject(currentFlowNode.getProperties(), FlowNodeProperties.class);
        String handler = FlowNodeTypeEnum.getHandler(flowNodeProperties.getBusinessType());
        if(StringUtils.isNotBlank(handler)){
            AbstractIFlowNodeHandler nodeHandler = SpringUtils.getBean(handler, AbstractIFlowNodeHandler.class);
            nodeHandler.businessHandler(event.getEvent(), flowData);
        }
    }

    /**
     * 是否支持异步
     *
     * @return
     */
    @Override
    public boolean supportsAsyncExecution() {
        return true;
    }
}
