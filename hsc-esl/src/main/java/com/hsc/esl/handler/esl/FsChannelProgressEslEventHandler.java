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

import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 对于呼出呼叫，另一方处于响铃状态；对于呼入电话，该方正在进行提醒。
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_PROGRESS)
@Component
public class FsChannelProgressEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if (Objects.isNull(callInfo)) {
            return;
        }
        // 信令时序诊断：180/183(无媒体)时间点，与 ANSWER 时间差用于推断网关接通判定方式(反极/声音检测)
        log.info("[信令时序] 对端振铃(180/183无媒体) callId:{} called:{} uniqueId:{}",
                callInfo.getCallId(), callInfo.getCallee(), uniqueId);
        // 被叫腿振铃 → 给坐席腿补发 180（晚应答架构下软电话收不到 B 腿信令，无此则打出后全程静默）
        notifyCallerRingReady(address, callInfo, uniqueId);
    }
}
