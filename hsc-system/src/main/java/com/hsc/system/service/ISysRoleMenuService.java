package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysRoleMenu;

import java.util.List;

/**
 * 角色菜单关联表(SysRoleMenu)表服务接口
 *
 * @author danmo
 * @since 2024-02-22 11:22:25
 */
public interface ISysRoleMenuService extends IBaseService<SysRoleMenu> {

    void updateBatch(Long roleId, List<Long> menuIds);
}

