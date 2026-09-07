// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.hsc.api.service.ILoginService;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.constant.SecurityConstants;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.constant.TokenConstants;
import com.hsc.security.authority.LoginUserInfo;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.system.domain.query.login.LoginQuery;
import com.hsc.system.domain.vo.login.LoginUserVo;
import com.hsc.system.service.ISysConfigService;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author danmo
 * @date 2024-02-21 15:13
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements ILoginService {

    private final AuthenticationManager authenticationManager;
    private final ISysUserService iSysUserService;
    private final RedisService redisService;
    private final LicenseService licenseService;
    private final ISysConfigService iSysConfigService;


    /**
     * 登录 token 有效期(分钟)默认值——sys_config 参数 session.token-expire-minutes 的兜底值，
     * 运行时以参数页配置为准(仅影响修改后新登录，已发 token 的 Redis TTL 不追溯)
     */
    private final static Integer EXPIRE_TIME = CacheConstants.EXPIRATION;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    @Override
    public LoginUserVo login(LoginQuery query) {
        // 授权闸：密码认证之前校验许可证（未授权/过期/licsrv 不可达一律拒绝，fail-closed）
        licenseService.checkAuthorizedForLogin();
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                query.getUsername(),
                query.getPassword()
        ));
        LoginUserInfo loginUserInfo = (LoginUserInfo) authenticate.getPrincipal();
        String jwtToken = JWT.create()
                .withClaim(SecurityConstants.USER_ID, loginUserInfo.getUserId())
                .withClaim(SecurityConstants.USER_NAME, loginUserInfo.getUsername())
                .withClaim(SecurityConstants.AVATAR, loginUserInfo.getAvatar())
                .withClaim(SecurityConstants.PHONE, loginUserInfo.getPhone())
                .withClaim(SecurityConstants.LOGIN_USER, JSONObject.toJSONString(loginUserInfo))
                .withClaim(SecurityConstants.TIME,DateUtil.current())
                .sign(Algorithm.HMAC512(TokenConstants.SECRET));
        // token 有效期读系统参数(参数页可调，改后仅影响新登录)
        int expireMinutes = iSysConfigService.getInt(SysConfigKeys.TOKEN_EXPIRE_MINUTES, EXPIRE_TIME);
        redisService.setCacheObject(ACCESS_TOKEN + loginUserInfo.getUserId(), jwtToken, expireMinutes, TimeUnit.MINUTES);
        // 登录请求不带 token，SecurityContext 尚未建立；写入线程上下文兜底，
        // 供"系统登陆"日志切面回填操作人（见 SecurityUtils.getCurrentUserInfo 的 ThreadLocal 兜底）
        SecurityUtils.setThreadLocalCurrentUserInfo(loginUserInfo);
        // 将token返回响应
        return LoginUserVo.builder()
                .accessToken(jwtToken)
                .expiresIn(expireMinutes)
                .build();
    }

    @Override
    public void logout() {
        Long userId = SecurityUtils.getUserId();
        // 登出会清空 SecurityContext，先备份到线程上下文，供"系统登出"日志切面回填操作人
        SecurityUtils.setThreadLocalCurrentUserInfo(SecurityUtils.getCurrentUserInfo());
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + userId);
        SecurityContextHolder.clearContext();
    }
}
