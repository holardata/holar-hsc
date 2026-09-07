package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.ChatPromptword;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 智能体提示词(chat_promptword)表数据库访问层
 */
@Repository()
@Mapper
public interface ChatPromptwordMapper extends BaseMapper<ChatPromptword> {
}
