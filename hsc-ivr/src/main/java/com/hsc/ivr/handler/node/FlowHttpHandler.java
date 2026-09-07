// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.Forest;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.http.ForestRequestType;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowHttpNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * HTTP节点处理
 *
 * @author danmo
 * @date 2024-12-26
 */

@Slf4j
@Component("FlowHttpHandler")
public class FlowHttpHandler extends AbstractIFlowNodeHandler {

    public FlowHttpHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
    }

    @Override
    public void execute(FlowDataContext flowData) throws FlowNodeException {
        log.info("HTTP节点处理 flowData：{}", flowData);
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            throw new FlowNodeException("节点配置错误");
        }
        FlowHttpNodeProperties flowHttpNodeProperties = JSONObject.parseObject(flowNode.getProperties(), FlowHttpNodeProperties.class);
        if (Objects.isNull(flowHttpNodeProperties)) {
            throw new FlowNodeException("节点配置条件错误");
        }
        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());
        if (Objects.isNull(callInfo)) {
            throw new FlowNodeException("callInfo is null");
        }
        if(StringUtils.isEmpty(flowHttpNodeProperties.getUrl())){
            throw new FlowNodeException("url is null");
        }
        if(Objects.isNull(flowHttpNodeProperties.getMethod())){
            throw new FlowNodeException("method is null");
        }
        ForestRequest<JSONObject> request = Forest.request(JSONObject.class);
        request.url();
        switch (flowHttpNodeProperties.getMethod()){
            case 1 -> {
                request.setType(ForestRequestType.GET);
                request.addQuery(JSONObject.parseObject(flowHttpNodeProperties.getParams()));
            }
            case 2 -> {
                request.setType(ForestRequestType.POST);
                request.addBody(JSONObject.parseObject(flowHttpNodeProperties.getParams()));
            }
            case 3 -> {
                request.setType(ForestRequestType.PUT);
                request.addBody(JSONObject.parseObject(flowHttpNodeProperties.getParams()));
            }
            case 4 -> {
                request.setType(ForestRequestType.DELETE);
                request.addQuery(JSONObject.parseObject(flowHttpNodeProperties.getParams()));
            }
        }
        if(CollectionUtils.isNotEmpty(flowHttpNodeProperties.getHeaders())){
            for (FlowHttpNodeProperties.HttpHeader header : flowHttpNodeProperties.getHeaders()) {
                request.addHeader(header.getName(), header.getValue());
            }
        }
        JSONObject result;
        try {
            result = request.executeAsFuture().getResponse().getResult();
        } catch (Exception e) {
            // 请求异常（网络/超时等）与结果为空同按失败处理，防 Forest 运行时异常穿透状态机
            log.error("HTTP节点请求异常 nodeId:{} url:{} error:{}", flowData.getCurrentNodeId(), flowHttpNodeProperties.getUrl(), e.getMessage(), e);
            result = null;
        }
        if (Objects.isNull(result)) {
            // 请求失败/结果为空：配了 event="fail" 出边（画布"请求失败跳转"）走失败分支，未配置维持结束流程
            noticeFail(flowData);
            return;
        }
        if (StringUtils.isNotEmpty(flowHttpNodeProperties.getResult())) {
            flowData.setHttpResult(result.getString(flowHttpNodeProperties.getResult()));
            callInfo.setFlowDataContext(flowData);
            fsCallCacheService.saveCallInfo(callInfo);
        }
        iFlowNoticeService.notice(2, "next", flowData);

    }
}
