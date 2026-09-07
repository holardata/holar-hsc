package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.FsCdr;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * CDR话单(fs_cdr)表数据库访问层
 */
@Repository()
@Mapper
public interface FsCdrMapper extends BaseMapper<FsCdr> {
}
