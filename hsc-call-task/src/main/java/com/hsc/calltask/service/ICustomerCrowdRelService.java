package com.hsc.calltask.service;

import com.hsc.calltask.domain.entity.CustomerCrowdRel;
import com.hsc.common.base.IBaseService;

import java.util.List;

/**
 * 人群客户关联表(CustomerCrowdRel)表服务接口
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
public interface ICustomerCrowdRelService extends IBaseService<CustomerCrowdRel> {

    void batchUpsert(List<Long> crowdIds, List<CustomerCrowdRel> relList);

}

