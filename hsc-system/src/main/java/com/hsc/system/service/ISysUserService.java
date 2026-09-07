// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysUser;
import com.hsc.system.domain.feature.IUserVo;
import com.hsc.system.domain.query.user.SysUserAddQuery;
import com.hsc.system.domain.query.user.SysUserQuery;
import com.hsc.system.domain.query.user.SysUserUpdateQuery;
import com.hsc.system.domain.vo.user.SysSimpleUserVo;
import com.hsc.system.domain.vo.user.SysUserVo;

import java.util.List;

/**
 * 用户信息表(User)表服务接口
 *
 * @author danmo
 * @since 2024-02-20 18:41:33
 */
public interface ISysUserService extends IBaseService<SysUser> {

    SysUser getByUsername(String username);

    SysUserVo getByUserId(Long userId);

    PageInfo<SysUserVo> getPageList(SysUserQuery query);

    List<SysUserVo> getList(SysUserQuery query);

    void decorate(IUserVo userVo);

    void decorate(List<? extends IUserVo> userList);

    void add(SysUserAddQuery query);

    void edit(SysUserUpdateQuery query);

    void editPassWord(SysUserAddQuery query);

    /**
     * 修改用户头像（当前登录人自助换，avatar 为 sys_file.id）
     */
    void editAvatar(Long userId, String avatar);

    /**
     * 修改当前登录用户资料（个人中心自助改昵称/手机/邮箱）
     */
    void editProfile(Long userId, SysUserUpdateQuery query);

    /**
     * 修改当前登录用户密码（验旧密码，成功后作废登录态强制重新登录）
     */
    void editPassword(Long userId, String oldPassword, String newPassword);

    void delete(SysUserQuery query);

    List<SysSimpleUserVo> getSelectList( SysUserQuery query);
}

