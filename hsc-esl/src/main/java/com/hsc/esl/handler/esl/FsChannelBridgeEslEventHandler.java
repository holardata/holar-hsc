// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.AgentStateEnum;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.utils.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 桥接调用
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_BRIDGE)
@Component
public class FsChannelBridgeEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        String otherUniqueId = EslEventUtil.getOtherUniqueId(event);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if(Objects.isNull(callInfo)){
            return;
        }
        if (callInfo.getAnswerTime() == null || callInfo.getAnswerTime() == 0L) {
            callInfo.setAnswerTime(event.getEventDateTimestamp() / 1000);
        }
        log.info("桥接成功 callId:{}, uniqueId:{}, otherUniqueId:{}", callInfo.getCallId(), uniqueId, otherUniqueId);
        ChannelInfo channelInfo = callInfo.getChannelMap().get(uniqueId);
        ChannelInfo otherChannelInfo = callInfo.getChannelMap().get(otherUniqueId);
        if (channelInfo != null && channelInfo.getBridgeTime() == null) {
            channelInfo.setBridgeTime(event.getEventDateTimestamp() / 1000);
        }
        if (otherChannelInfo != null && otherChannelInfo.getBridgeTime() == null) {
            otherChannelInfo.setBridgeTime(event.getEventDateTimestamp() / 1000);
        }

        // MRCP ASR 已废弃：线上统一用旁路 ASR(下方 forkAudioToAsrBot → ai-callbot 流式 ASR)。
        // 且 FsDetectedSpeechEslEventHandler 是空实现(识别结果只 log 不落库)，保留这两行只会向 FS
        // 发无用的 unimrcp 命令(FS 未配 MRCP 时刷错误日志)。如需恢复 MRCP，解开下面两行。
        // fsClient.detectSpeech(address,callInfo.getCallId(),uniqueId);
        // fsClient.detectSpeech(address,callInfo.getCallId(),otherUniqueId);

        // 🆕 旁路 ASR（方案：mod_audio_fork WS 推流给 ai-callbot）
        // 双路：客户路(role=user) + 坐席路(role=agent) 各推一路单方音频 → 说话人分离。
        // 仅人工通话段需要（转真人坐席的话）。AI 接听段 ai-callbot 自己做 ASR+落库(source=ai)，
        // 若再旁路会把 AI 的 TTS 又识别一遍，造成转写重复 → AI 接听段（任一腿是 AI 智能坐席）不推。
        String callIdStr = String.valueOf(callInfo.getCallId());
        boolean isAiLeg = (channelInfo != null && EslConstant.AI_AGENT_NAME.equals(channelInfo.getAgentName()))
                || (otherChannelInfo != null && EslConstant.AI_AGENT_NAME.equals(otherChannelInfo.getAgentName()));
        if (!isAiLeg) {
            fsClient.forkAudioToAsrBot(address, callIdStr, uniqueId, "user", callInfo.getAgentNumber());
            fsClient.forkAudioToAsrBot(address, callIdStr, otherUniqueId, "agent", callInfo.getAgentNumber());
        }

        // 写回时判空：otherUniqueId 来自事件头，缓存里未必有对应通道，塞 null 会污染 channelMap
        if (channelInfo != null) {
            callInfo.setChannelInfoMap(uniqueId, channelInfo);
        }
        if (otherChannelInfo != null) {
            callInfo.setChannelInfoMap(otherUniqueId, otherChannelInfo);
        }
        ifsCallCacheService.saveCallInfo(callInfo);

        sendAgentStatus(callInfo.getCallId(),callInfo.getCaller(),callInfo.getCallee(),callInfo.getDirection(), AgentStateEnum.TALKING);
    }
}
