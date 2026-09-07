// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.job;


import com.hsc.calltask.domain.CustomerCrowdEvent;
import com.hsc.calltask.domain.CustomerCrowdEventParam;
import com.hsc.calltask.domain.query.CustomerCrowdQuery;
import com.hsc.calltask.domain.vo.CustomerCrowdVo;
import com.hsc.calltask.service.ICustomerCrowdService;
import com.hsc.common.utils.TraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 客户人群定时任务
 *
 * @author danmo
 * @date 2025/7/2 14:32
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class CustomerCrowdJob extends QuartzJobBean {

    @Autowired
    private ICustomerCrowdService customerCrowdService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            TraceUtil.setTraceId(UUID.randomUUID().toString().replaceAll("-", ""));
            log.info("开始执行客户人群任务");
            CustomerCrowdQuery query = new CustomerCrowdQuery();
            query.setStatus(1);
            query.setType(2);
            List<CustomerCrowdVo> customerCrowdList = customerCrowdService.getList(query);
            if(CollectionUtils.isEmpty(customerCrowdList)){
                return;
            }
            log.info("客户人群任务开始执行，数量：{}", customerCrowdList.size());
            for (CustomerCrowdVo customerCrowd : customerCrowdList) {
                CustomerCrowdEventParam param = new CustomerCrowdEventParam();
                param.setCrowdId(customerCrowd.getId());
                param.setEventType(3);
                applicationContext.publishEvent(new CustomerCrowdEvent(param));
            }
        } catch (Exception e) {
            log.error("客户人群任务执行异常", e);
            throw new JobExecutionException("客户人群任务执行异常", e);
        }finally {
            TraceUtil.clear();
        }
    }
}
