// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;


import cn.hutool.core.collection.CollectionUtil;
import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.utils.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;
import com.hsc.common.utils.StringUtils;

/**
 * 渠道对呼叫完成执行某些操作处理类
 *
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_EXECUTE_COMPLETE)
@Component
public class FsChannelExecuteCompleteEslEventHandler extends AbstractFsEslEventHandler {

    private final IFlowNoticeService iFlowNoticeService;

    public FsChannelExecuteCompleteEslEventHandler(IFlowNoticeService iFlowNoticeService) {
        this.iFlowNoticeService = iFlowNoticeService;
    }

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        if (StringUtils.isBlank(EslEventUtil.getApplication(event))) {
            return;
        }
        String uniqueId = EslEventUtil.getUniqueId(event);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if (callInfo == null) {
            return;
        }
        log.info("executeComplete callId:{} uniqueId:{} app:{}", callInfo.getCallId(), uniqueId, EslEventUtil.getApplication(event));
        switch (EslEventUtil.getApplication(event)) {
            case "playback":
                if ("FILE PLAYED".equals(EslEventUtil.getApplicationResponse(event))) {
                    log.info("uniqueId:{}, playback:{} success", uniqueId, EslEventUtil.getApplicationData(event));
                    // 结束语音播完挂断：IVR 结束节点配了挂机前语音时置的标记（hangup 与 playback
                    // 同批下发会被秒杀，故延迟到此回调；标记用后即清防重复挂断）
                    if (Boolean.TRUE.equals(callInfo.getEndPlaybackHangup())) {
                        callInfo.setEndPlaybackHangup(false);
                        ifsCallCacheService.saveCallInfo(callInfo);
                        log.info("结束语音播完，挂断 callId:{}", callInfo.getCallId());
                        fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
                        return;
                    }
                    if (CollectionUtil.isNotEmpty(callInfo.getDetailList())){
                        CallInfoDetail callInfoDetail = callInfo.getDetailList().get(0);
                        if (callInfoDetail.getTransferType() == 2) {
                            iFlowNoticeService.notice(2,"next", callInfo.getFlowDataContext());
                        }
                    }
/*                    if (callInfo.getQueueStartTime() != null && callInfo.getQueueEndTime() == null) {
                        fsClient.playFile(address, uniqueId, "queue.wav");
                        return;
                    }*/

                } else if ("FILE NOT FOUND".equals(EslEventUtil.getApplicationResponse(event))) {
                    log.error("uniqueId:{}  file:{} not found", uniqueId, EslEventUtil.getApplicationData(event));
                    fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
                    return;
                }
                break;

            case "play_and_get_digits":
                //菜单/收号/满意度各自 var_name，按 currentNodeId 路由到对应节点 handler
                String digitsReturn = EslEventUtil.getDigitsReturn(event);
                iFlowNoticeService.notice(4, digitsReturn, callInfo.getFlowDataContext());
                break;
            case "break":
                break;

            default:
                break;
        }
        log.debug("execute:{}, data:{}, response:{}", EslEventUtil.getApplication(event), EslEventUtil.getApplicationData(event), EslEventUtil.getApplicationResponse(event));
    }
}
