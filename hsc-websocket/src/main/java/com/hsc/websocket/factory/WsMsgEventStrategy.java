package com.hsc.websocket.factory;


import com.hsc.websocket.domain.WsMsgPayload;

/**
 * @author danmo
 * @date 2025/05/27 22:12
 */
public interface WsMsgEventStrategy {

    void handler(Long userId, WsMsgPayload payload);
}
