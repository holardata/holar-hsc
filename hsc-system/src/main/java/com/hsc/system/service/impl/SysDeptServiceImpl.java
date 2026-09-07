package com.hsc.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.SysDept;
import com.hsc.system.mapper.SysDeptMapper;
import com.hsc.system.service.ISysDeptService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门信息(sys_dept)表服务实现（dept-management 1F）。
 *
 * <p>简化：spec 提的闭包表(sys_dept_relation)维护复杂，此处后代查询用 parent_id 内存递归
 * （单实例部门数据量小，性能可接受）；tree 用 hutool TreeUtil（同 SysCategory.treeList 范式）。
 */
@Service
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    @Override
    public void add(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(0);
        }
        save(dept);
    }

    @Override
    public void edit(SysDept dept) {
        if (dept.getDeptId() == null) {
            throw new CommonException("部门ID不能为空");
        }
        updateById(dept);
    }

    @Override
    public void delete(Long deptId) {
        SysDept del = new SysDept();
        del.setDeptId(deptId);
        del.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        updateById(del);
    }

    @Override
    public SysDept getDetail(Long deptId) {
        return getById(deptId);
    }

    @Override
    public List<Tree<Long>> treeList(String deptName) {
        // 统一带 del_flag=0 过滤（delFlag 无 @TableLogic，手动过滤，避免逻辑删除后仍查出）
        if (StrUtil.isNotBlank(deptName)) {
            // 按名称模糊命中
            List<SysDept> matched = list(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                    .like(SysDept::getDeptName, deptName)
                    .orderByAsc(SysDept::getOrder));
            if (matched.isEmpty()) {
                return new ArrayList<>();
            }
            // 补全命中节点的所有祖先 id（保留树结构，父级链一路向上）
            Set<Long> keepIds = new HashSet<>();
            for (SysDept m : matched) {
                keepIds.add(m.getDeptId());
            }
            List<SysDept> all = list(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                    .orderByAsc(SysDept::getOrder));
            for (SysDept m : matched) {
                Long pid = m.getParentId();
                while (pid != null && pid != 0L && keepIds.add(pid)) {
                    final Long currentPid = pid;
                    SysDept parent = all.stream()
                            .filter(d -> d.getDeptId().equals(currentPid))
                            .findFirst().orElse(null);
                    if (parent == null) {
                        break;
                    }
                    pid = parent.getParentId();
                }
            }
            final Set<Long> finalKeepIds = keepIds;
            List<SysDept> list = all.stream()
                    .filter(d -> finalKeepIds.contains(d.getDeptId()))
                    .collect(Collectors.toList());
            return buildDeptTreeNodes(list);
        }

        List<SysDept> list = list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .orderByAsc(SysDept::getOrder));
        return buildDeptTreeNodes(list);
    }

    /** 扁平部门列表 → hutool Tree（id=deptId、parentId、deptName/order/status/remark/createTime）。 */
    private List<Tree<Long>> buildDeptTreeNodes(List<SysDept> list) {
        TreeNodeConfig config = new TreeNodeConfig();
        config.setIdKey("deptId");
        config.setWeightKey("order");
        config.setParentIdKey("parentId");
        return TreeUtil.build(list, 0L, config, (dept, treeNode) -> {
            treeNode.setId(dept.getDeptId());
            treeNode.setParentId(dept.getParentId());
            treeNode.putExtra("deptName", dept.getDeptName());
            treeNode.putExtra("order", dept.getOrder());
            treeNode.putExtra("status", dept.getStatus());
            treeNode.putExtra("remark", dept.getRemark());
            // createTime 脱离字段上下文进 Tree extra 后，BaseEntity 上的 @JsonFormat 失效，
            // Jackson 走默认 ISO8601；此处格式化为字符串，与全库 yyyy-MM-dd HH:mm:ss 出口一致。
            treeNode.putExtra("createTime",
                    dept.getCreateTime() == null ? null
                            : DateUtil.format(dept.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
        });
    }

    @Override
    public List<SysDept> getDescendants(Long deptId) {
        List<SysDept> all = list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .orderByAsc(SysDept::getOrder));
        List<SysDept> descendants = new ArrayList<>();
        collectDescendants(all, deptId, descendants);
        return descendants;
    }

    private void collectDescendants(List<SysDept> all, Long parentId, List<SysDept> result) {
        for (SysDept d : all) {
            if (parentId.equals(d.getParentId())) {
                result.add(d);
                collectDescendants(all, d.getDeptId(), result);
            }
        }
    }

    @Override
    public void lockDept(Long deptId) {
        updateStatus(deptId, 1);
    }

    @Override
    public void unlockDept(Long deptId) {
        updateStatus(deptId, 0);
    }

    private void updateStatus(Long deptId, Integer status) {
        SysDept update = new SysDept();
        update.setDeptId(deptId);
        update.setStatus(status);
        updateById(update);
    }
}
