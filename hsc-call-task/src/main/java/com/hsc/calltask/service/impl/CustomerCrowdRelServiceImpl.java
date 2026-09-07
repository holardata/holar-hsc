// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.calltask.mapper.CustomerCrowdRelMapper;
import com.hsc.calltask.domain.entity.CustomerCrowdRel;
import com.hsc.calltask.service.ICustomerCrowdRelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 人群客户关联表(CustomerCrowdRel)表服务实现类
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@Service
public class CustomerCrowdRelServiceImpl extends BaseServiceImpl<CustomerCrowdRelMapper, CustomerCrowdRel> implements ICustomerCrowdRelService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchUpsert(List<Long> crowdIds, List<CustomerCrowdRel> relList) {
        // crowdIds 是集合，必须用 in；误用 eq 会把 List 整体当单值绑参(JDBC 序列化成 CollSer 字节流)，MySQL 报 Truncated incorrect INTEGER value
        remove(new LambdaUpdateWrapper<CustomerCrowdRel>().in(CustomerCrowdRel::getCrowdId,crowdIds));
        saveBatch(relList);
    }

}

