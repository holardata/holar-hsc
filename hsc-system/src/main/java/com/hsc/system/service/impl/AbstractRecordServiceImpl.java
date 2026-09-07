package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.mapper.AbstractRecordMapper;
import com.hsc.system.service.IAbstractRecordService;
import org.springframework.stereotype.Service;

/**
 * 通话过程摘要(abstract_record)表服务实现
 */
@Service
public class AbstractRecordServiceImpl extends BaseServiceImpl<AbstractRecordMapper, AbstractRecord> implements IAbstractRecordService {
}
