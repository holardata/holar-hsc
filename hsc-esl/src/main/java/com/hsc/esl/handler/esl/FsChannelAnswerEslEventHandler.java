// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.handler.call.FsAbstractCallProcess;
import com.hsc.esl.utils.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 渠道回答处理类
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_ANSWER)
@Component
public class FsChannelAnswerEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        log.info("ChannelAnswerEslEventHandler handle address:{} uniqueId:{}", address, uniqueId);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if(Objects.isNull(callInfo)){
            return;
        }
        // 信令时序诊断：200 OK 到达时间点，与 PROGRESS/PROGRESS_MEDIA 时间差用于推断网关接通判定方式(反极/声音检测)
        log.info("[信令时序] 对端应答(200 OK) callId:{} called:{} uniqueId:{}",
                callInfo.getCallId(), callInfo.getCallee(), uniqueId);
        ChannelInfo channelInfo = callInfo.getChannelMap().get(uniqueId);
        if (Objects.isNull(channelInfo)) {
            return;
        }
        channelInfo.setAnswerTime(event.getEventDateTimestamp()/1000);
        channelInfo.setRingEndTime(event.getEventDateTimestamp() / 1000);
        callInfo.setAnswerCount(callInfo.getAnswerCount() + 1);
        FsAbstractCallProcess factory = fsEslProcessFactory.factory(callInfo.getProcess());
        try {
            factory.handler(address,event,callInfo,channelInfo);
        } catch (Exception e) {
            log.error("应答处理类执行异常 callId:{}", callInfo.getCallId(), e);
        }
    }
}
