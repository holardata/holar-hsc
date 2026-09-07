package com.hsc.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysDept;

import java.util.List;

/**
 * 部门信息(sys_dept)表服务接口（dept-management 1F）。
 *
 * <p>平迁 rb SysDept 部门 CRUD/树形/后代/解锁锁定，适配 hsc sys_dept 表。
 */
public interface ISysDeptService extends IBaseService<SysDept> {

    void add(SysDept dept);

    void edit(SysDept dept);

    void delete(Long deptId);

    SysDept getDetail(Long deptId);

    /**
     * 部门树(按 parent_id 嵌套)。
     *
     * @param deptName 部门名称(模糊匹配，可空)。非空时返回命中节点及其祖先链(保证树结构完整)。
     */
    List<Tree<Long>> treeList(String deptName);

    /** 获取指定部门的所有后代(经 parent_id 递归)。 */
    List<SysDept> getDescendants(Long deptId);

    /** 停用部门(status=1)。 */
    void lockDept(Long deptId);

    /** 启用部门(status=0)。 */
    void unlockDept(Long deptId);
}
