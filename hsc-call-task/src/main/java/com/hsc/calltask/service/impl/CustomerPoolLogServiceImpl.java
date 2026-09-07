package com.hsc.calltask.service.impl;

import com.hsc.calltask.domain.entity.CustomerPoolLog;
import com.hsc.calltask.mapper.CustomerPoolLogMapper;
import com.hsc.calltask.service.ICustomerPoolLogService;
import com.hsc.common.base.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 客户流转日志表(CustomerPoolLog)表服务实现类
 *
 * @author danmo
 * @since 2026-09-03
 */
@Service
public class CustomerPoolLogServiceImpl extends BaseServiceImpl<CustomerPoolLogMapper, CustomerPoolLog> implements ICustomerPoolLogService {

}
