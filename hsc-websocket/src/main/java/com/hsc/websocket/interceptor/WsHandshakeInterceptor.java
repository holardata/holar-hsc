// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.websocket.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.utils.StringUtils;
import com.hsc.security.authority.LoginUserInfo;
import com.hsc.security.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * @author danmo
 * @date 2023年09月22日 11:26
 */
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final RedisService redisService;

    public WsHandshakeInterceptor(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            String token = ((ServletServerHttpRequest) request).getServletRequest().getParameter("token");
            final String username;
            if (StringUtils.isBlank(token)) {
                return false;
            }
            // 从token中解析出userId
            Long userId = JwtUtils.getUserId(token);
            String jwtToken = (String) redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + userId);
            if (StringUtils.isEmpty(jwtToken) && !StringUtils.equals(token, jwtToken)) {
                return false;
            }
            LoginUserInfo userDetails = JSONObject.parseObject(JwtUtils.getLoginUserInfo(token), LoginUserInfo.class);
            attributes.put("user", userDetails);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }


}
