package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.HscAreaCode;
import com.hsc.system.mapper.HscAreaCodeMapper;
import com.hsc.system.service.IHscAreaCodeService;
import org.springframework.stereotype.Service;

/**
 * 基于location_gaode手工整理后的表(用于匹配区号)(HscAreaCode)表服务实现类
 *
 * @author danmo
 * @since 2025-05-26 17:10:03
 */
@Service
public class HscAreaCodeServiceImpl extends BaseServiceImpl<HscAreaCodeMapper, HscAreaCode> implements IHscAreaCodeService {

}

