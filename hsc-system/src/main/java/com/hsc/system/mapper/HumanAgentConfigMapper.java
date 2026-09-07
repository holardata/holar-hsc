package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.HumanAgentConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 人工坐席助手配置(human_agent_config)表数据库访问层
 */
@Repository()
@Mapper
public interface HumanAgentConfigMapper extends BaseMapper<HumanAgentConfig> {
}
