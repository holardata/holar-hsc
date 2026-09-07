// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysCategory;
import com.hsc.system.domain.query.category.SysCategoryAddQuery;
import com.hsc.system.domain.query.category.SysCategoryQuery;

import java.util.List;

/**
 * 分类配置表(LfsCategory)表服务接口
 *
 * @author danmo
 * @since 2023-10-31 11:06:55
 */
public interface ISysCategoryService extends IBaseService<SysCategory> {

    void add(SysCategoryAddQuery query);

    void edit(SysCategoryAddQuery query);

    void delete(SysCategoryQuery query);

    SysCategory getDetail(Long id);

    List<Tree<Long>> treeList(SysCategoryQuery query);
}

