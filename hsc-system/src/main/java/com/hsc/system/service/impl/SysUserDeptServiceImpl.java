package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.SysUserDept;
import com.hsc.system.mapper.SysUserDeptMapper;
import com.hsc.system.service.ISysUserDeptService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户部门关联表(sys_user_dept)服务实现。
 *
 * <p>简化：updateBatch 删旧全删 + 插新（leader 默认 0，不启用负责人）。
 */
@Service
public class SysUserDeptServiceImpl extends BaseServiceImpl<SysUserDeptMapper, SysUserDept> implements ISysUserDeptService {

    @Override
    public void updateBatch(Long userId, List<Long> deptIds) {
        // 删旧关联（逻辑删除 del_flag=1）
        update(new LambdaUpdateWrapper<SysUserDept>()
                .set(SysUserDept::getDelFlag, 1)
                .eq(SysUserDept::getUserId, userId));
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        // 插新关联（leader 默认 0）
        List<SysUserDept> list = deptIds.stream().map(deptId -> {
            SysUserDept ud = new SysUserDept();
            ud.setUserId(userId);
            ud.setDeptId(deptId);
            ud.setLeader(0);
            return ud;
        }).collect(Collectors.toList());
        saveBatch(list);
    }
}
