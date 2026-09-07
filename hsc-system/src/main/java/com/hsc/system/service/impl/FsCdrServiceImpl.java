package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.FsCdr;
import com.hsc.system.mapper.FsCdrMapper;
import com.hsc.system.service.IFsCdrService;
import org.springframework.stereotype.Service;

/**
 * CDR话单(fs_cdr)表服务实现
 */
@Service
public class FsCdrServiceImpl extends BaseServiceImpl<FsCdrMapper, FsCdr> implements IFsCdrService {
}
