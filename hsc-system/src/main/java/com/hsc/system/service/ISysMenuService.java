// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysMenu;
import com.hsc.system.domain.query.menu.SysMenuAddQuery;
import com.hsc.system.domain.query.menu.SysMenuQuery;
import com.hsc.system.domain.vo.menu.RouterVo;

import java.util.List;

/**
 * 菜单权限表(SysMenu)表服务接口
 *
 * @author danmo
 * @since 2024-02-22 11:22:25
 */
public interface ISysMenuService extends IBaseService<SysMenu> {

    void add(SysMenuAddQuery query);

    void edit(SysMenuAddQuery query);

    List<SysMenu> getList(SysMenuQuery query);

    void delete(SysMenuQuery query);

    List<Tree<Long>> buildMenuTree(List<SysMenu> list);

    /**
     * 将 SysMenu 列表构建为 vben 前端动态路由结构。
     * <ul>
     *   <li>剔除停用（status=1）与按钮（menuType=F）节点；父节点被剔除时其子节点一并丢弃；</li>
     *   <li>目录（M）component 留空，菜单（C）component 取 views 下路径；</li>
     *   <li>name 按完整 path 生成 PascalCase，保证全局唯一；path 输出绝对路径；</li>
     *   <li>visible=1 映射为 meta.hideInMenu，order_num 映射为 meta.order 并据此排序。</li>
     * </ul>
     *
     * @param list 已按角色过滤的 SysMenu 列表
     * @return vben 路由树
     */
    List<RouterVo> buildVbenRouters(List<SysMenu> list);

    List<SysMenu> getMenuListByRoleIds(List<Long> roleIds);
}

