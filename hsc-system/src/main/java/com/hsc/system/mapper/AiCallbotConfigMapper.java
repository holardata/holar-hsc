package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.AiCallbotConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * AI 坐席配置(ai_callbot_config)表数据库访问层
 */
@Repository()
@Mapper
public interface AiCallbotConfigMapper extends BaseMapper<AiCallbotConfig> {
}
