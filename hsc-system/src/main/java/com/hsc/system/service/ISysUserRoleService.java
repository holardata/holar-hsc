package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysUserRole;

import java.util.List;

/**
 * 用户角色关联表(SysUserRole)表服务接口
 *
 * @author danmo
 * @since 2024-02-22 11:22:25
 */
public interface ISysUserRoleService extends IBaseService<SysUserRole> {

    void updateBatch(Long userId, List<Long> roleIds);
}

