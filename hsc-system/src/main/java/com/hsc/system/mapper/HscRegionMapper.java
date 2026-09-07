package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import com.hsc.system.domain.entity.HscRegion;

/**
 * (HscRegion)表数据库访问层
 *
 * @author danmo
 * @since 2025-05-26 17:11:09
 */
@Repository()
@Mapper
public interface HscRegionMapper extends BaseMapper<HscRegion> {

}

