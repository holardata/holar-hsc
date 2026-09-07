package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysUserDept;

import java.util.List;

/**
 * 用户部门关联表(sys_user_dept)服务接口
 */
public interface ISysUserDeptService extends IBaseService<SysUserDept> {

    /**
     * 重置用户的部门关联（删旧插新；leader 预留，默认 0）。
     */
    void updateBatch(Long userId, List<Long> deptIds);
}
