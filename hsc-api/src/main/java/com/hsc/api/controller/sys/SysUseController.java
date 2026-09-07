// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.sys;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.common.enums.ExceptionStatusEnum;
import com.hsc.common.exception.RequestException;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.system.domain.entity.SysMenu;
import com.hsc.system.domain.query.menu.SysMenuQuery;
import com.hsc.system.domain.query.user.SysUserAddQuery;
import com.hsc.system.domain.query.user.SysUserQuery;
import com.hsc.system.domain.query.user.SysUserUpdateQuery;
import com.hsc.system.domain.query.user.UserProfilePasswordQuery;
import com.hsc.system.domain.vo.menu.RouterVo;
import com.hsc.system.domain.vo.user.SysSimpleUserVo;
import com.hsc.system.domain.vo.user.SysUserVo;
import com.hsc.system.service.ISysMenuService;
import com.hsc.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author danmo
 * @date 2024-02-21 17:16
 **/
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/v1/user")
public class SysUseController extends BaseController {

    @Autowired
    private ISysUserService iSysUserService;

    @Autowired
    private ISysMenuService iSysMenuService;

    @Log(title = "新增用户", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:user:add')")
    @Operation(summary = "新增用户", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated SysUserAddQuery query) {
        iSysUserService.add(query);
        return success();
    }

    @Log(title = "修改用户", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:user:edit')")
    @Operation(summary = "修改用户", method = "POST")
    @PostMapping("/edit/{userId}")
    public ResResult edit(@PathVariable("userId") Long userId, @RequestBody @Validated SysUserUpdateQuery query) {
        query.setUserId(userId);
        iSysUserService.edit(query);
        return success();
    }

    @Log(title = "修改用户密码", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:user:editPassWord')")
    @Operation(summary = "修改用户密码", method = "POST", description = "只传密码字段")
    @PostMapping("/editPassWord/{userId}")
    public ResResult editPassWord(@PathVariable("userId") Long userId, @RequestBody SysUserAddQuery query) {
        query.setUserId(userId);
        iSysUserService.editPassWord(query);
        return success();
    }

    /**
     * 修改当前登录用户头像（个人中心自助换）。
     * 登录即可访问（无 @PreAuthorize，同 userInfo 定位，避免 sys_menu F 按钮依赖）；
     * 不加 @Validated（SysUserUpdateQuery.userName 带 @NotEmpty，会强制传 userName），
     * 非空校验下沉 service 手写，同 editPassWord 范式；userId 一律取当前登录人，防越权改他人。
     */
    @Log(title = "修改个人头像", businessType = BusinessTypeEnum.UPDATE)
    @Operation(summary = "修改当前登录用户头像", method = "POST", description = "只传 avatar 字段（sys_file.id）")
    @PostMapping("/profile/avatar")
    public ResResult profileAvatar(@RequestBody SysUserUpdateQuery query) {
        iSysUserService.editAvatar(SecurityUtils.getUserId(), query.getAvatar());
        return success();
    }

    /**
     * 修改当前登录用户资料（个人中心自助改昵称/手机/邮箱）。
     * 登录即可访问（同 profile/avatar 定位）；校验同款下沉 service 手写；userId 取当前登录人防越权。
     */
    @Log(title = "修改个人资料", businessType = BusinessTypeEnum.UPDATE)
    @Operation(summary = "修改当前登录用户资料", method = "POST", description = "只传 nickName/phone/email 字段")
    @PostMapping("/profile/edit")
    public ResResult profileEdit(@RequestBody SysUserUpdateQuery query) {
        iSysUserService.editProfile(SecurityUtils.getUserId(), query);
        return success();
    }

    /**
     * 修改当前登录用户密码（验旧密码）。成功后作废登录态，前端引导重新登录。
     */
    @Log(title = "修改个人密码", businessType = BusinessTypeEnum.UPDATE)
    @Operation(summary = "修改当前登录用户密码", method = "POST")
    @PostMapping("/profile/password")
    public ResResult profilePassword(@RequestBody @Validated UserProfilePasswordQuery query) {
        iSysUserService.editPassword(SecurityUtils.getUserId(), query.getOldPassword(), query.getNewPassword());
        return success();
    }

    @Log(title = "删除用户", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:user:delete')")
    @Operation(summary = "删除用户", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody SysUserQuery query) {
        iSysUserService.delete(query);
        return success();
    }

    @Log(title = "获取用户详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:user:get')")
    @Operation(summary = "获取用户详情", method = "POST")
    @PostMapping("/get")
    public ResResult<SysUserVo> get(@RequestBody SysUserQuery query) {
        if (Objects.isNull(query.getUserId())) {
            throw new RequestException(ExceptionStatusEnum.ERROR_USERID_NOT_NULL.getCode(), ExceptionStatusEnum.ERROR_USERID_NOT_NULL.getMsg());
        }
        SysUserVo sysUserVo = iSysUserService.getByUserId(query.getUserId());
        return success(sysUserVo);
    }

    @Log(title = "获取用户列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:user:list')")
    @Operation(summary = "获取用户列表", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<SysUserVo>> getPageList(@RequestBody SysUserQuery query) {
        PageInfo<SysUserVo> pageInfo = iSysUserService.getPageList(query);
        return success(pageInfo);
    }

    @Log(title = "获取用户下拉列表", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "获取用户下拉列表", method = "POST")
    @PostMapping("/select/list")
    public ResResult<List<SysSimpleUserVo>> getSelectList(@RequestBody SysUserQuery query) {
        return success(iSysUserService.getSelectList(query));
    }

    @Log(title = "获取用户菜单", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "获取用户菜单", method = "GET")
    @GetMapping("getRouters")
    public ResResult<List<RouterVo>> getRouters() {
        // 角色信息
        List<SysMenu> menus = new LinkedList<>();
        List<Long> roles = SecurityUtils.getRole();
        if ((!CollectionUtils.isEmpty(roles) && roles.contains(1L)) || SecurityUtils.getUserId() == 1L) {
            SysMenuQuery query = new SysMenuQuery();
            menus.addAll(iSysMenuService.getList(query));
        } else {
            menus.addAll(iSysMenuService.getMenuListByRoleIds(roles));
        }
        List<RouterVo> routers = iSysMenuService.buildVbenRouters(menus);
        return success(routers);
    }
}
