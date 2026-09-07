package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.domain.query.engine.VoiceEngineQuery;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 语音引擎实例表(VoiceEngine)表数据库访问层
 */
@Repository
@Mapper
public interface VoiceEngineMapper extends BaseMapper<VoiceEngine> {

    List<VoiceEngine> getList(VoiceEngineQuery query);
}
