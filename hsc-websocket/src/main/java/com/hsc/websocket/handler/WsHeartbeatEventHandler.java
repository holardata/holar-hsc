// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.websocket.handler;


import com.hsc.common.annotation.WsEventName;
import com.hsc.websocket.domain.WsMsgPayload;
import com.hsc.websocket.factory.AbstractWsMsgEventStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2025/05/27 22:20
 */
@WsEventName(name = "HEARTBEAT")
@Slf4j
@AllArgsConstructor
@Component
public class WsHeartbeatEventHandler extends AbstractWsMsgEventStrategy {


    @Override
    protected void doHandler(Long userId, WsMsgPayload payload) {

    }
}
