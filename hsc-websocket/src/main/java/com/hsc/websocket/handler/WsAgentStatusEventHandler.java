// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.websocket.handler;


import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.WsEventName;
import com.hsc.system.service.ISipAgentService;
import com.hsc.websocket.domain.WsMsgPayload;
import com.hsc.websocket.factory.AbstractWsMsgEventStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2025/05/27 22:20
 */
@WsEventName(name = "AGENT_STATUS")
@Slf4j
@AllArgsConstructor
@Component
public class WsAgentStatusEventHandler extends AbstractWsMsgEventStrategy {

    private final ISipAgentService iSipAgentService;

    @Override
    public void doHandler(Long userId, WsMsgPayload payload) {
        log.info("WsAgentStatusEventHandler handle");
        WsAgentStatusData data = JSONObject.parseObject(payload.getData(), WsAgentStatusData.class);
        iSipAgentService.updateOnlineStatus(data.getAgentId(), data.getOnlineStatus(), payload.getTimestamp());
    }


    @Data
    public static class WsAgentStatusData {
        private Long agentId;
        private Integer onlineStatus;
    }
}
