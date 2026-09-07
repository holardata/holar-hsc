package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.SiteConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 站点配置(site_config)表数据库访问层
 */
@Repository()
@Mapper
public interface SiteConfigMapper extends BaseMapper<SiteConfig> {
}
