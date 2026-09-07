// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslEventNames;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.utils.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

/**
 * 放音结束
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.PLAYBACK_STOP)
@Component
public class FsChannelPlaybackEndEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        log.info("放音结束 uniqueId:{} file:{}", EslEventUtil.getUniqueId(event), EslEventUtil.getApplicationData(event));
    }
}
