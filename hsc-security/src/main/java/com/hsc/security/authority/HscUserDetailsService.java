// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.security.authority;

import com.hsc.system.domain.entity.SysUser;
import com.hsc.system.domain.vo.menu.SysMenuVo;
import com.hsc.system.domain.vo.role.SysRoleVo;
import com.hsc.system.service.ISysRoleService;
import com.hsc.system.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author danmo
 * @date 2024-07-10 15:00
 **/
@Component
public class HscUserDetailsService implements UserDetailsService {

    @Autowired
    private  ISysUserService iSysUserService;
    @Autowired
    private  ISysRoleService iSysRoleService;

    @Override
    public LoginUserInfo loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = iSysUserService.getByUsername(username);
        if (Objects.isNull(sysUser)) {
            throw new UsernameNotFoundException("用户不存在！");
        }

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId(sysUser.getUserId());
        loginUserInfo.setUsername(sysUser.getUserName());
        loginUserInfo.setNickName(sysUser.getNickName());
        loginUserInfo.setPassword(sysUser.getPassword());
        loginUserInfo.setAvatar(sysUser.getAvatar());
        loginUserInfo.setSex(sysUser.getSex());
        loginUserInfo.setPhone(sysUser.getPhone());
        loginUserInfo.setDeptId(sysUser.getDeptId());

        if (Objects.equals(1, sysUser.getStatus())) {
            loginUserInfo.setEnabled(true);
        }
        List<SysRoleVo> roleList = iSysRoleService.getRoleByUserId(sysUser.getUserId());
        List<Long> roleIds = Optional.ofNullable(roleList).orElseGet(ArrayList::new).stream()
                .filter(role -> Objects.equals(0, role.getStatus())).map(SysRoleVo::getRoleId).collect(Collectors.toList());
        loginUserInfo.setRoleIds(roleIds);

        Set<Integer> dataScopes = Optional.ofNullable(roleList).orElseGet(ArrayList::new).stream()
                .filter(role -> Objects.equals(0, role.getStatus())).map(SysRoleVo::getDataScope).collect(Collectors.toSet());
        loginUserInfo.setDataScope(new ArrayList<>(dataScopes));

        Set<HscGrantedAuthority> authorities = Optional.ofNullable(roleList).orElseGet(ArrayList::new).stream()
                .filter(role -> Objects.equals(0, role.getStatus())).map(role -> new HscGrantedAuthority(role.getRoleKey(), role.getMenuList().stream().map(SysMenuVo::getPerms).filter(StringUtils::isNotBlank).collect(Collectors.toList()))).collect(Collectors.toSet());
        loginUserInfo.setAuthorities(authorities);


        loginUserInfo.setAccountNonExpired(true);
        loginUserInfo.setCredentialsNonExpired(true);
        loginUserInfo.setAccountNonLocked(true);
        return loginUserInfo;
    }
}
