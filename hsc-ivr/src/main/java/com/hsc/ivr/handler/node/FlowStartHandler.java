// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowStartNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.service.IVoiceEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;


/**
 * 开始节点处理
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowStartHandler")
public class FlowStartHandler extends AbstractIFlowNodeHandler {

    @Autowired
    private IVoiceEngineService iVoiceEngineService;

    public FlowStartHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
    }

    @Override
    public void execute(FlowDataContext flowData) throws FlowNodeException {
        try {
            FlowNodeVo flowNode = getFlowNode(flowData);
            String properties = flowNode.getProperties();
            if (StringUtils.isNotBlank(properties)) {
                FlowStartNodeProperties startNodeProperties = JSONObject.parseObject(properties, FlowStartNodeProperties.class);
                // unify-voice-engine-config：asrEngine/ttsEngine 存 voice_engine 实例 id，
                // 启动时解析实例塞流程上下文（TTS 另存参数指纹，预合成缓存 key 用）
                if(Objects.nonNull(startNodeProperties.getAsrEngine())){
                    try {
                        iVoiceEngineService.resolve(startNodeProperties.getAsrEngine());
                        flowData.setAsrEngineId(startNodeProperties.getAsrEngine());
                    } catch (Exception e) {
                        log.warn("开始节点 ASR 引擎实例不可用 id={}：{}", startNodeProperties.getAsrEngine(), e.getMessage());
                    }
                }
                if(Objects.nonNull(startNodeProperties.getTtsEngine())){
                    try {
                        VoiceEngine ttsEngine = iVoiceEngineService.resolve(startNodeProperties.getTtsEngine());
                        flowData.setTtsEngineId(ttsEngine.getId());
                        flowData.setTtsFingerprint(iVoiceEngineService.fingerprint(ttsEngine));
                    } catch (Exception e) {
                        log.warn("开始节点 TTS 引擎实例不可用 id={}：{}", startNodeProperties.getTtsEngine(), e.getMessage());
                    }
                }
            }
            iFlowNoticeService.notice(2, "next", flowData);
        } catch (Exception e) {
            log.error("开始节点处理异常", e);
            throw new FlowNodeException(e);
        }
    }

}
