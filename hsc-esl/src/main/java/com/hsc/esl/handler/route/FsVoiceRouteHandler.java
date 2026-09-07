// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.date.DateUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@EslRouteName(RouteTypeEnum.VOICE)
@Component
@Slf4j
public class FsVoiceRouteHandler extends FsAbstractRouteHandler {


    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String voiceId) {
        log.info("转放音 callId:{} transfer to {}", callInfo.getCallId(), voiceId);
        VoiceFileVo voiceFileVo = iVoiceFileService.getDetail(Long.valueOf(voiceId));
        if(Objects.isNull(voiceFileVo)){
            log.info("转放音获取文件失败 callId:{} transfer to {}", callInfo.getCallId(), voiceId);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }

        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(4);

        // FS playback 传 sounds 相对路径(getUuidName 从 filePath 截掉 /temp/voice/ 前缀，如 2026/8/12/uuid.mp3)。
        // 后端 /temp/voice 与 FS /usr/local/freeswitch/sounds 是共享挂载(docker volume)，文件天然同步，无需推送。
        // 不要传 filePath(后端磁盘绝对路径，FS 容器里没有)。
        fsClient.playFile(address, uniqueId, voiceFileVo.getUuidName());

        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
    }
}
