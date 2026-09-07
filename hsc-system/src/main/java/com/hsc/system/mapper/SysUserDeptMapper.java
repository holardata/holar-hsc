package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.SysUserDept;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 用户部门关联表(sys_user_dept)数据库访问层
 */
@Repository
@Mapper
public interface SysUserDeptMapper extends BaseMapper<SysUserDept> {

}
