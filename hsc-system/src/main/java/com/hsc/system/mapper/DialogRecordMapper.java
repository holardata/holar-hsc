package com.hsc.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.system.domain.entity.DialogRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 通话逐句对话记录(dialog_record)表数据库访问层
 */
@Repository()
@Mapper
public interface DialogRecordMapper extends BaseMapper<DialogRecord> {
}
