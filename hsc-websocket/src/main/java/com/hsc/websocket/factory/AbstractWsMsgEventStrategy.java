// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.websocket.factory;


import com.hsc.common.utils.StringUtils;
import com.hsc.security.authority.LoginUserInfo;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.websocket.domain.WsMsgPayload;
import com.hsc.websocket.service.IWebSocketService;
import com.hsc.websocket.utils.WsSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * @author danmo
 * @date 2025/05/29 10:29
 */
@Slf4j
@Component
public abstract class AbstractWsMsgEventStrategy implements WsMsgEventStrategy{

    @Autowired
    private IWebSocketService webSocketService;


    @Override
    public void handler(Long userId, WsMsgPayload payload) {
        beforeHandler(userId);
        try {
            //todo 可以加线程执行
            doHandler(userId, payload);
        } catch (Exception e) {
            log.error("handler error userId:{},payload:{}", userId, payload, e);
        }
        afterHandler(userId);
    }

    protected abstract void doHandler(Long userId, WsMsgPayload payload);

    protected void beforeHandler(Long userId) {
        String sessionId = webSocketService.getSessionId(String.valueOf(userId));
        if(StringUtils.isNotBlank(sessionId)){
            WebSocketSession webSocketSession = WsSessionUtil.get(sessionId);
            if(Objects.nonNull(webSocketSession)){
                Object user = webSocketSession.getAttributes().get("user");
                if(Objects.nonNull(user) && user instanceof LoginUserInfo userInfo){
                    SecurityUtils.setThreadLocalCurrentUserInfo(userInfo);
                }
            }
        }
    }

    protected void afterHandler(Long userId) {
        SecurityUtils.removeThreadLoCurrentUserInfo();
    }
}
