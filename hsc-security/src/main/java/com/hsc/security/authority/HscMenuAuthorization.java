// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.security.authority;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author danmo
 * @date 2024-02-23 10:44
 **/
@Component("authz")
public class HscMenuAuthorization {

    public boolean hasPerm(String permission) {
        List<HscGrantedAuthority> authorities = (List<HscGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        for (HscGrantedAuthority authority : authorities) {
            if (authority.getPermissions().contains(permission) || StringUtils.equals(authority.getAuthority(), "ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
