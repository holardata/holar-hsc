package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.hsc.system.domain.entity.HscAreaCode;

/**
 * 基于location_gaode手工整理后的表(用于匹配区号)(HscAreaCode)表数据库访问层
 *
 * @author danmo
 * @since 2025-05-26 17:10:01
 */
@Repository()
@Mapper
public interface HscAreaCodeMapper extends BaseMapper<HscAreaCode> {

}

