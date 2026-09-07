// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller;

import com.hsc.api.service.ILoginService;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.common.enums.OperatorTypeEnum;
import com.hsc.security.authority.LoginUserInfo;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.system.domain.entity.SysUser;
import com.hsc.system.domain.query.login.LoginQuery;
import com.hsc.system.domain.vo.login.LoginUserVo;
import com.hsc.system.domain.vo.login.UserInfoVo;
import com.hsc.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author danmo
 * @date 2024-02-21 15:08
 **/
@Tag(name = "授权登录")
@RestController
@RequestMapping("/auth/v1")
public class LoginController extends BaseController {

    @Autowired
    private ILoginService iLoginService;

    @Autowired
    private ISysUserService iSysUserService;

    @Log(title = "系统登陆",businessType = BusinessTypeEnum.LOGIN,operatorType = OperatorTypeEnum.MANAGE)
    @Operation(summary = "系统登陆", method = "POST")
    @PostMapping("/login")
    public ResResult<LoginUserVo> login(@Validated @RequestBody LoginQuery query) {
        return success(iLoginService.login(query));
    }

    @Log(title = "系统登出",businessType = BusinessTypeEnum.LOGOUT,operatorType = OperatorTypeEnum.MANAGE)
    @Operation(summary = "系统登出", method = "GET")
    @GetMapping("/logout")
    public ResResult logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        iLoginService.logout();
        logoutHandler.logout(request, response, authentication);
        return success();
    }

    @Operation(summary = "获取当前登录用户信息", method = "GET")
    @GetMapping("/userInfo")
    public ResResult<UserInfoVo> userInfo() {
        // 登录即可访问（无 @PreAuthorize，仿 login/logout）；从 SecurityContext 取当前用户
        LoginUserInfo loginUserInfo = SecurityUtils.getCurrentUserInfo();
        UserInfoVo vo = new UserInfoVo();
        vo.setUserId(loginUserInfo.getUserId());
        vo.setUsername(loginUserInfo.getUsername());
        // 个人资料字段查库取最新值：SecurityContext 里的是登录时刻快照（烙进 JWT claim），
        // 个人中心改完头像/昵称/手机不重登录立即生效；查不到（用户已被删）回退快照兜底
        SysUser sysUser = iSysUserService.getById(loginUserInfo.getUserId());
        if (Objects.nonNull(sysUser)) {
            vo.setNickName(sysUser.getNickName());
            vo.setAvatar(sysUser.getAvatar());
            vo.setSex(sysUser.getSex());
            vo.setPhone(sysUser.getPhone());
            vo.setEmail(sysUser.getEmail());
        } else {
            vo.setNickName(loginUserInfo.getNickName());
            vo.setAvatar(loginUserInfo.getAvatar());
            vo.setSex(loginUserInfo.getSex());
            vo.setPhone(loginUserInfo.getPhone());
        }
        vo.setRoleIds(loginUserInfo.getRoleIds());
        return success(vo);
    }
}
