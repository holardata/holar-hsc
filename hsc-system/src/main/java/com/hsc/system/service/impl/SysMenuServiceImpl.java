// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.system.domain.entity.SysMenu;
import com.hsc.system.domain.query.menu.SysMenuAddQuery;
import com.hsc.system.domain.query.menu.SysMenuQuery;
import com.hsc.system.domain.vo.menu.RouterVo;
import com.hsc.system.mapper.SysMenuMapper;
import com.hsc.system.service.ISysMenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单权限表(SysMenu)表服务实现类
 *
 * @author danmo
 * @since 2024-02-22 11:22:25
 */
@Service
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Override
    public void add(SysMenuAddQuery query) {
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(query, sysMenu);
        save(sysMenu);
    }

    @Override
    public void edit(SysMenuAddQuery query) {
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(query, sysMenu);
        sysMenu.setMenuId(query.getMenuId());
        updateById(sysMenu);
    }

    @Override
    public List<SysMenu> getList(SysMenuQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public void delete(SysMenuQuery query) {
        Optional.ofNullable(query.getMenuIds()).orElseGet(ArrayList::new).add(query.getMenuId());
        if (!CollectionUtils.isEmpty(query.getMenuIds())) {
            update(new LambdaUpdateWrapper<SysMenu>().set(SysMenu::getDelFlag, DeleteStatusEnum.DELETE_YES.getIndex())
                    .and(item -> item.in(SysMenu::getMenuId, query.getMenuIds())).or().in(SysMenu::getParentId,query.getMenuIds()));
        }
    }

    @Override
    public List<Tree<Long>> buildMenuTree(List<SysMenu> list) {
        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();
        //自定义属性名 都要默认值的
        treeNodeConfig.setWeightKey("orderNum");
        treeNodeConfig.setIdKey("menuId");
        treeNodeConfig.setParentIdKey("parentId");
        return TreeUtil.build(list, 0L, treeNodeConfig, ((sysMenu, treeNode) -> {
            treeNode.setId(sysMenu.getMenuId());//id
            treeNode.setParentId(sysMenu.getParentId());//父id
            treeNode.putExtra("name", sysMenu.getMenuName());
            treeNode.putExtra("orderNum", sysMenu.getOrderNum());
            treeNode.putExtra("menuType", sysMenu.getMenuType());
            treeNode.putExtra("component", sysMenu.getComponent());
            treeNode.putExtra("iconName", sysMenu.getIcon());
            treeNode.putExtra("isFrame", sysMenu.getIsFrame());
            treeNode.putExtra("perms", sysMenu.getPerms());
            treeNode.putExtra("visible", sysMenu.getVisible());
            treeNode.putExtra("path", sysMenu.getPath());
            treeNode.putExtra("status", sysMenu.getStatus());
            treeNode.putExtra("remark", sysMenu.getRemark());
        }));
    }

    @Override
    public List<SysMenu> getMenuListByRoleIds(List<Long> roleIds) {
        return this.baseMapper.getMenuListByRoleIds(roleIds);
    }

    @Override
    public List<RouterVo> buildVbenRouters(List<SysMenu> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 1. 过滤：剔除停用（status=1）与按钮（F）
        List<SysMenu> kept = list.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 0)
                .filter(m -> !"F".equals(m.getMenuType()))
                .collect(Collectors.toList());
        if (kept.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> keptIds = kept.stream().map(SysMenu::getMenuId).collect(Collectors.toSet());

        // 2. 组装父子关系：parentId=0 为根；父在保留集合内挂为子节点；父被剔除则丢弃孤儿
        Map<Long, List<SysMenu>> childrenMap = new HashMap<>();
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : kept) {
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(menu);
            } else if (keptIds.contains(parentId)) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menu);
            }
        }

        // 3. 按 order_num 排序
        Comparator<SysMenu> byOrder = Comparator
                .comparingInt(m -> m.getOrderNum() == null ? Integer.MAX_VALUE : m.getOrderNum());
        roots.sort(byOrder);
        childrenMap.values().forEach(c -> c.sort(byOrder));

        // 4. 自顶向下构建 RouterVo 树，逐层解析绝对 path 并生成 name
        List<RouterVo> result = new ArrayList<>();
        for (SysMenu root : roots) {
            // 顶级目录 path 约定为绝对路径（如 /system），空则按 menuId 兜底
            result.add(buildRouterVo(root, resolvePath(root), childrenMap));
        }
        return result;
    }

    /**
     * 菜单 path 空兜底：菜单管理表单对目录(M)隐藏 path 字段，历史提交会把存量 path 清空。
     * 空顶级 path 会让路由 name 退化、前端目录点击无目标，这里按 menuId 生成稳定路径：
     * 顶级补绝对路径（/menu-{id}），子级补相对路径（menu-{id}，由 joinPath 拼到父级下）。
     */
    private String resolvePath(SysMenu menu) {
        String p = menu.getPath();
        if (p != null && !p.isBlank()) {
            return p;
        }
        return (menu.getParentId() == null || menu.getParentId() == 0L)
                ? "/menu-" + menu.getMenuId()
                : "menu-" + menu.getMenuId();
    }

    /**
     * 递归构建单个 RouterVo。
     *
     * @param menu        当前菜单
     * @param fullPath    当前菜单解析后的绝对路径（父级绝对路径 + 本级相对 path）
     * @param childrenMap 父子关系映射
     */
    private RouterVo buildRouterVo(SysMenu menu, String fullPath,
                                   Map<Long, List<SysMenu>> childrenMap) {
        RouterVo vo = new RouterVo();
        vo.setName(toRouteName(fullPath, menu.getMenuId()));
        vo.setPath(fullPath);

        // 目录（M）component 留空，交给前端按首子路由推导 redirect；菜单（C）取 views 下路径
        if ("C".equals(menu.getMenuType())) {
            vo.setComponent(menu.getComponent());
        }

        RouterVo.Meta meta = new RouterVo.Meta();
        meta.setTitle(menu.getMenuName());
        if (menu.getIcon() != null && !menu.getIcon().isEmpty()) {
            meta.setIcon(menu.getIcon());
        }
        if (menu.getOrderNum() != null) {
            meta.setOrder(menu.getOrderNum());
        }
        if (menu.getVisible() != null && menu.getVisible() == 1) {
            meta.setHideInMenu(true);
        }
        // is_frame: 0 是外链
        if (menu.getIsFrame() != null && menu.getIsFrame() == 0) {
            meta.setLink(menu.getPath());
        }
        vo.setMeta(meta);

        List<SysMenu> children = childrenMap.get(menu.getMenuId());
        if (children != null && !children.isEmpty()) {
            List<RouterVo> childVos = new ArrayList<>(children.size());
            for (SysMenu child : children) {
                childVos.add(buildRouterVo(child, joinPath(fullPath, resolvePath(child)), childrenMap));
            }
            vo.setChildren(childVos);
        }
        return vo;
    }

    /**
     * 拼接父子 path：子级为绝对路径直接用，否则父级绝对路径 + '/' + 子级相对 path。
     */
    private String joinPath(String parentPath, String childPath) {
        if (childPath == null || childPath.isEmpty()) {
            return parentPath;
        }
        if (childPath.startsWith("/")) {
            return childPath;
        }
        if (parentPath == null || parentPath.isEmpty()) {
            return "/" + childPath;
        }
        if (parentPath.endsWith("/")) {
            return parentPath + childPath;
        }
        return parentPath + "/" + childPath;
    }

    /**
     * 由完整 path 生成 PascalCase 路由 name：/system/user -> SystemUser。
     * path 无有效段（空、"/"）时按 menuId 兜底——绝不能返回 "Root"：前端 vben 会把后端
     * 顶级路由挂到内部根路由（name=Root）下，同名会抛 "A route named Root has been added
     * as a child..."，路由初始化失败、页面打不开。
     */
    private String toRouteName(String fullPath, Long menuId) {
        StringBuilder sb = new StringBuilder();
        // 同时按斜杠和横杠切分：kebab-case path（如 /calling/phone-number）→ CallingPhoneNumber
        for (String seg : fullPath.split("/|-")) {
            if (seg.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
        }
        return sb.length() == 0 ? "Menu" + menuId : sb.toString();
    }
}

