package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.SysRole;
import com.hsc.system.domain.query.role.SysRoleQuery;
import com.hsc.system.domain.vo.role.SysRoleVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色信息表(SysRole)表数据库访问层
 *
 * @author danmo
 * @since 2024-02-22 11:22:25
 */
@Repository()
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {


    List<SysRoleVo> getRoleByUserId(@Param("userId") Long userId);

    SysRoleVo getDetail(SysRoleQuery query);

    List<SysRoleVo> getList(SysRoleQuery query);
}

