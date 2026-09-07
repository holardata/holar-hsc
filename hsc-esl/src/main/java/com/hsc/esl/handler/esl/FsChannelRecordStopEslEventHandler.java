// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.domain.CallInfo;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.utils.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

/**
 * 结束录音
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.RECORD_STOP)
@Component
public class FsChannelRecordStopEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if (callInfo == null) {
            return;
        }
        log.info("录音结束 callId:{} uniqueId:{}", callInfo.getCallId(), uniqueId);
        callInfo.setRecordEndTime(event.getEventDateTimestamp() / 1000);
        if (callInfo.getRecordStartTime() != null) {
            callInfo.setRecordTime(callInfo.getRecordEndTime() - callInfo.getRecordStartTime());
        }
        ifsCallCacheService.saveCallInfo(callInfo);
    }
}
