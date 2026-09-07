// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import com.alibaba.fastjson.JSONObject;
import cn.hutool.core.date.DateUtil;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowPlaybackNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 放音节点处理
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowPlaybackHandler")
public class FlowPlaybackHandler extends AbstractIFlowNodeHandler {

    private final IVoiceFileService iVoiceFileService;

    public FlowPlaybackHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, IVoiceFileService iVoiceFileService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.iVoiceFileService = iVoiceFileService;
    }

    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)){
            throw new FlowNodeException("节点配置错误");
        }
        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());
        if (Objects.isNull(callInfo)){
            throw new FlowNodeException("callInfo is null");
        }
        callInfo.setFlowDataContext(flowData);
        fsCallCacheService.saveCallInfo(callInfo);
        String properties = flowNode.getProperties();
        if (StringUtils.isNotBlank(properties)){
            FlowPlaybackNodeProperties playbackNodeProperties = JSONObject.parseObject(properties, FlowPlaybackNodeProperties.class);
            // 任意键打断放音（playback_terminators，仅对纯 playback 生效）：开=any 按键跳过播报；关=none 听完
            applyInterrupt(flowData, playbackNodeProperties.getInterrupt());
            fsClient.sendArgs(flowData.getAddress(), flowData.getUniqueId(), EslConstant.SET, EslConstant.PLAYBACK_DELIMITER);
            if (playbackNodeProperties.getPlaybackType() == 1){
                VoiceFileVo voiceFile = iVoiceFileService.getDetail(playbackNodeProperties.getFileId());
                StringBuilder fileName = new StringBuilder(voiceFile.getUuidName());
                if(playbackNodeProperties.getNum() > 1){
                    for (int i = 1; i < playbackNodeProperties.getNum(); i++){
                        fileName.append(EslConstant.EXCLAMATION).append(voiceFile.getUuidName());
                    }
                }
                fsClient.playFile(flowData.getAddress(), flowData.getUniqueId(), fileName.toString());
                // IVR 交互转写：播报文字=语音文件的 speech_text（无则文件名）
                ivrDialogLogger.logPlay(flowData, StringUtils.isNotBlank(voiceFile.getSpeechText())
                        ? voiceFile.getSpeechText() : voiceFile.getName());
            }else if (playbackNodeProperties.getPlaybackType() == 2){
                // 文本放音（${变量} 混播 + 预合成缓存，unify-voice-engine-config：替代 say:/MRCP 死链路）
                String cacheFile = resolvePlayFile(iVoiceFileService, flowData, 2,
                        playbackNodeProperties.getFileId(), playbackNodeProperties.getContent());
                StringBuilder fileName = new StringBuilder(cacheFile);
                if (playbackNodeProperties.getNum() > 1){
                    for (int i = 1; i < playbackNodeProperties.getNum(); i++){
                        fileName.append(EslConstant.EXCLAMATION).append(cacheFile);
                    }
                }
                fsClient.playFile(flowData.getAddress(), flowData.getUniqueId(), fileName.toString());
                // IVR 交互转写：播报文字=渲染后的文本内容
                ivrDialogLogger.logPlay(flowData, renderTtsContent(flowData, playbackNodeProperties.getContent()));
            }
            //记 transferType=2 的 detail：playback 完成回调（FsChannelExecuteCompleteEslEventHandler）
            //据此流转 next。不写 detail 则放完流程卡死（同 FlowTransferHandler case5 机制）。
            CallInfoDetail detail = new CallInfoDetail();
            detail.setCallId(flowData.getCallId());
            detail.setStartTime(DateUtil.current());
            detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
            detail.setTransferType(2);
            callInfo.addDetailList(detail);
            fsCallCacheService.saveCallInfo(callInfo);
        }

    }

    @Override
    public void businessHandler(String event, FlowDataContext flowData) throws FlowNodeException {

    }
}
