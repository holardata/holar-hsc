package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * LLM模型配置(model_config)表数据库访问层
 */
@Repository()
@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {
}
