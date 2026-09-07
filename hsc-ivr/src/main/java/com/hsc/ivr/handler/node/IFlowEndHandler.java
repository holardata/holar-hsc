// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowEndNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 结束节点处理
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowEndHandler")
public class IFlowEndHandler extends AbstractIFlowNodeHandler {

    private final IVoiceFileService iVoiceFileService;

    public IFlowEndHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, IVoiceFileService iVoiceFileService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.iVoiceFileService = iVoiceFileService;
    }

    @Override
    public void execute(FlowDataContext flowData) {
        log.info("结束节点处理 flowData：{}", flowData);
        FlowNodeVo flowNode = getFlowNode(flowData);
        if(Objects.isNull(flowNode)){
            throw new FlowNodeException("结束节点配置错误");
        }
        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());
        if (Objects.isNull(callInfo)){
            throw new FlowNodeException("callInfo is null");
        }
        callInfo.getDetailList().forEach(detail -> {
            if (Objects.equals(detail.getTransferType(), 2)){
                detail.setEndTime(DateUtil.current());
            }
        });
        callInfo.setDetailList(callInfo.getDetailList());
        callInfo.setFlowDataContext(flowData);

        boolean hangUp = false;
        String endFile = null;
        String properties = flowNode.getProperties();
        FlowEndNodeProperties endNodeProperties = null;
        if (StringUtils.isNotBlank(properties)){
            endNodeProperties = JSONObject.parseObject(properties, FlowEndNodeProperties.class);
            hangUp = Boolean.TRUE.equals(endNodeProperties.getHangUp());
            if (hangUp){
                endFile = resolvePlayFile(iVoiceFileService, flowData, endNodeProperties.getEndPlaybackType(),
                        endNodeProperties.getEndFileId(), renderTtsContent(flowData, endNodeProperties.getEndContent()));
            }
        }
        boolean hasEndVoice = hangUp && StringUtils.isNotBlank(endFile) && !SILENCE_PROMPT.equals(endFile);
        if (hasEndVoice){
            log.info("挂机前播放结束语音 callId:{} file:{}", flowData.getCallId(), endFile);
            //挂断延迟到播放完成事件回调(FsChannelExecuteCompleteEslEventHandler 查 endPlaybackHangup
            //标记挂断)——hangup app 到达 FS 即生效不等排队，与 playback 同批下发会把语音秒杀
            callInfo.setEndPlaybackHangup(true);
        } else if (hangUp){
            // 静音兜底跳过播放：未配置(endPlaybackType=null，旧版发布数据)/文件已删/文本合成失败均会走到这
            log.warn("挂机前结束语音未播放(未配置或合成失败)，直接挂断 callId:{} endFile:{}", flowData.getCallId(), endFile);
        }
        fsCallCacheService.saveCallInfo(callInfo);
        if (hasEndVoice){
            // IVR 交互转写：结束播报（真实下发播放才落）
            ivrDialogLogger.logPlay(flowData, resolvePlayText(iVoiceFileService, endNodeProperties.getEndPlaybackType(),
                    endNodeProperties.getEndFileId(), renderTtsContent(flowData, endNodeProperties.getEndContent())));
            fsClient.playFile(flowData.getAddress(), flowData.getUniqueId(), endFile);
        } else if (hangUp){
            fsClient.hangupCall(flowData.getAddress(), flowData.getCallId(), flowData.getUniqueId());
        }
    }

}
