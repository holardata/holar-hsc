
package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslEventNames;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

/**
 * 创建外拨呼叫
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_OUTGOING)
@Component
public class FsChannelOutGoingEslEventHandler extends AbstractFsEslEventHandler {

    @Override
    public void handleEslEvent(String address, EslEvent event) {
       // log.info("ChannelOutGoingEslEventHandler handle address:{} EslEvent:{}.", address, JSONObject.toJSONString(event));
    }
}
