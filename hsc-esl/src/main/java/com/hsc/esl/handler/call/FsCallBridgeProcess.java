// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.call;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.hsc.common.annotation.EslProcessName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

/**
 * 桥接
 *
 * @author danmo
 * @date 2023-10-23 17:05
 **/
@EslProcessName(ProcessEnum.CALL_BRIDGE)
@Component
@Slf4j
public class FsCallBridgeProcess extends FsAbstractCallProcess {

    @Override
    public void handler(String address, EslEvent event, CallInfo callInfo, ChannelInfo lfsChannelInfo) {
        log.info("[早应答] CALL_BRIDGE 触发 callId:{} 触发腿uniqueId:{} otherUniqueId:{}(主叫腿)",
                callInfo.getCallId(), lfsChannelInfo.getUniqueId(), lfsChannelInfo.getOtherUniqueId());
        ChannelInfo channel = callInfo.getChannelMap().get(lfsChannelInfo.getOtherUniqueId());
        ChannelInfo otherChannel = callInfo.getChannelMap().get(lfsChannelInfo.getUniqueId());
        if (channel == null || otherChannel == null) {
            log.warn("[早应答] CALL_BRIDGE 腿缺失 return callId:{} channelNull:{} otherChannelNull:{}",
                    callInfo.getCallId(), channel == null, otherChannel == null);
            return;
        }
        // 防循环：answer 主叫会触发主叫 CHANNEL_ANSWER 再进 CALL_BRIDGE，此时已 bridge 则跳过
        if (channel.getBridgeTime() != null && otherChannel.getBridgeTime() != null) {
            log.info("[早应答] CALL_BRIDGE 已bridge防循环 return callId:{}", callInfo.getCallId());
            return;
        }
        if (channel.getBridgeTime() == null) {
            channel.setBridgeTime(event.getEventDateTimestamp() / 1000);
        }
        if (otherChannel.getBridgeTime() == null) {
            otherChannel.setBridgeTime(event.getEventDateTimestamp() / 1000);
        }

        // 早应答改造：人工路由主叫未 answer（延迟到坐席接听），这里 promote 到 active 才能 bridge + 录音；
        // AI 路由主叫已 answer（answer 幂等 no-op）。answer 触发的主叫 CHANNEL_ANSWER 由上方防循环挡。
        log.info("[早应答] CALL_BRIDGE answer主叫(promote人工延迟answer) callId:{} 主叫腿:{}",
                callInfo.getCallId(), lfsChannelInfo.getOtherUniqueId());
        fsClient.answer(address, lfsChannelInfo.getOtherUniqueId());

        // 外呼等未在路由阶段录音的通话，bridge 时补录（呼入已在 FsCallIRouteProcess 录，record 非空不重复）
        if (StrUtil.isBlank(callInfo.getRecord())) {
            String filePath = sysSettingConfig.getFsProfile() + "/" + DateUtil.today() + "/"
                    + callInfo.getCallId() + "_" + DateUtil.current() + sysSettingConfig.getFsFileSuffix();
            fsClient.record(address, callInfo.getCallId(), lfsChannelInfo.getUniqueId(), filePath);
            callInfo.setRecord(filePath);
            log.info("[早应答] CALL_BRIDGE 补录 callId:{} record:{}", callInfo.getCallId(), filePath);
        }

        callInfo.setChannelInfoMap(lfsChannelInfo.getOtherUniqueId(),channel);
        callInfo.setChannelInfoMap(lfsChannelInfo.getUniqueId(),otherChannel);
        lfsCallCacheService.saveCallInfo(callInfo);
        log.info("[早应答] CALL_BRIDGE bridgeCall callId:{} 主叫腿:{} 触发腿:{}",
                callInfo.getCallId(), lfsChannelInfo.getOtherUniqueId(), lfsChannelInfo.getUniqueId());
        // 停主叫腿的回铃音（AI 转人工时 endless_playback 放的嘟嘟），避免漏到正式通话
        fsClient.playBreak(address, lfsChannelInfo.getOtherUniqueId());
        fsClient.bridgeCall(address, callInfo.getCallId(), lfsChannelInfo.getOtherUniqueId(), lfsChannelInfo.getUniqueId());
        // AI 转人工 bridge 成功：清 aiTransferring 并落盘（后续挂断走正常收尾，不进重试分支）
        callInfo.setAiTransferring(false);
        lfsCallCacheService.saveCallInfo(callInfo);
    }

}
