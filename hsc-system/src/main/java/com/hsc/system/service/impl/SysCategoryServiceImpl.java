// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.FsDialplan;
import com.hsc.system.domain.entity.SysCategory;
import com.hsc.system.domain.query.category.SysCategoryAddQuery;
import com.hsc.system.domain.query.category.SysCategoryQuery;
import com.hsc.system.mapper.FsDialplanMapper;
import com.hsc.system.mapper.SysCategoryMapper;
import com.hsc.system.service.ISysCategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类配置表(SysCategory)表服务实现类
 *
 * @author danmo
 * @since 2023-10-31 11:06:55
 */
@Service
public class SysCategoryServiceImpl extends BaseServiceImpl<SysCategoryMapper, SysCategory> implements ISysCategoryService {

    @Resource
    private FsDialplanMapper fsDialplanMapper;

    @Override
    public void add(SysCategoryAddQuery query) {
        SysCategory lfsCategory = checkName(query);
        if (null != lfsCategory) {
            throw new CommonException("名称已存在！");
        }
        SysCategory category = new SysCategory();
        BeanUtil.copyProperties(query, category);
        save(category);
    }


    @Override
    public void edit(SysCategoryAddQuery query) {
        //判断是否存在相同的名称
        SysCategory lfsCategory = checkName(query);
        if (null != lfsCategory) {
            throw new CommonException("名称已存在！");
        }
        SysCategory category = new SysCategory();
        BeanUtil.copyProperties(query, category);
        category.setId(query.getId());
        updateById(category);
    }

    @Override
    public void delete(SysCategoryQuery query) {
        if (query.getId() == null) {
            throw new CommonException("分类ID不能为空");
        }
        SysCategory category = getById(query.getId());
        if (category == null) {
            throw new CommonException("分类不存在或已删除");
        }
        if (category.getFlag() == 1) {
            throw new CommonException("分类不可删除");
        }
        // 校验分组下是否存在资源，有则禁止删除：删分组若不清组下资源会留下孤儿数据，
        // 而 xml_curl 查 fs_dialplan 不校验 group_id，孤儿混入解析会让软电话互打 404。
        // 收集待删分组 id（本分组 + 一级子分组），与下方 update 删除范围保持一致。
        // 当前覆盖拨号计划(fs_dialplan)；IVR/技能组/意图待后续按策略接口补。
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(query.getId());
        list(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getParentId, query.getId())
                .select(SysCategory::getId))
                .forEach(child -> groupIds.add(child.getId()));
        Long dialplanCount = fsDialplanMapper.selectCount(new LambdaQueryWrapper<FsDialplan>()
                .in(FsDialplan::getGroupId, groupIds)
                .eq(FsDialplan::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (dialplanCount != null && dialplanCount > 0) {
            throw new CommonException("分组下存在拨号计划，请先删除后再删除分组");
        }
        SysCategory delCategory = new SysCategory();
        delCategory.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        update(delCategory, new LambdaQueryWrapper<SysCategory>()
                .and(item -> item.eq(SysCategory::getId, query.getId()).or().eq(SysCategory::getParentId, query.getId())));
    }

    @Override
    public SysCategory getDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<Tree<Long>> treeList(SysCategoryQuery query) {
        List<SysCategory> list = new ArrayList<>();
        SysCategory weCategoryVo = new SysCategory();
        weCategoryVo.setId(0L);
        weCategoryVo.setName("默认分组");
        weCategoryVo.setFlag(1);
        weCategoryVo.setParentId(0L);
        list.add(0, weCategoryVo);

        List<SysCategory> weCategories = baseMapper.categoryList(query.getType());
        list.addAll(weCategories);

        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();
        //自定义属性名 都要默认值的
        treeNodeConfig.setIdKey("id");
        treeNodeConfig.setWeightKey("id");
        treeNodeConfig.setParentIdKey("parentId");
        return TreeUtil.build(list, 0L, treeNodeConfig, ((category, treeNode) -> {
            treeNode.setId(category.getId());//id
            treeNode.setParentId(category.getParentId());//父id
            treeNode.putExtra("title", category.getName());
            treeNode.putExtra("type", category.getType());
            treeNode.putExtra("flag", category.getFlag());
            treeNode.putExtra("key", category.getId());
        }));
    }

    private SysCategory checkName(SysCategoryAddQuery query) {
        //判断是否存在相同的名称
        return this.getOne(
                new LambdaQueryWrapper<SysCategory>().eq(SysCategory::getType, query.getType())
                        .eq(SysCategory::getName, query.getName()).eq(SysCategory::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                        .eq(SysCategory::getParentId, query.getParentId()));
    }
}

