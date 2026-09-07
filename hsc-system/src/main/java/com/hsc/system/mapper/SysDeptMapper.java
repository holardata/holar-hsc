package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 部门信息(sys_dept)表数据库访问层
 */
@Repository()
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
}
