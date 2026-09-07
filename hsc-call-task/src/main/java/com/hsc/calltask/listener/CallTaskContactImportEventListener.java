// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.listener;


import com.alibaba.fastjson2.JSONObject;
import com.hsc.calltask.domain.CallTaskContactImportEvent;
import com.hsc.calltask.domain.entity.CallTaskAssignment;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.service.ICallTaskAssignmentService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.utils.TraceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * 任务联系人导入监听器
 *
 * @author danmo
 * @date 2025/7/2 22:19
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class CallTaskContactImportEventListener implements ApplicationListener<CallTaskContactImportEvent> {

    private final ICustomerSeasService iCustomerSeasService;
    private final ICallTaskAssignmentService iCallTaskAssignmentService;


    @Override
    public void onApplicationEvent(CallTaskContactImportEvent event) {
        TraceUtil.setTraceId(UUID.randomUUID().toString().replaceAll("-", ""));
        try {
            log.info("任务联系人导入监听器 event:{}", JSONObject.toJSONString(event));
            if (Objects.equals(0, event.getType())) {
                crowdImport(event);
            } else if (Objects.equals(1, event.getType())) {
                fileImport(event);
            }
        } finally {
            TraceUtil.clear();
        }
    }


    private void crowdImport(CallTaskContactImportEvent event) {
        try {
            CustomerSeasVo customerSeas = iCustomerSeasService.getDetail(event.getCustomerId());
            CallTaskAssignment assignment = new CallTaskAssignment();
            assignment.setTaskId(event.getTaskId());
            assignment.setPhone(customerSeas.getPhone());
            assignment.setName(customerSeas.getName());
            assignment.setExt(customerSeas.getCustomerInfo().toJSONString());
            assignment.setSource(0);
            assignment.setCrowdId(event.getCrowdId());
            assignment.setCustomerId(event.getCustomerId());
            // 公海客户的模板：ext 字段标识 → 显示名/类型翻译依据（文件导入同带）
            assignment.setTemplateId(customerSeas.getTemplateId());
            assignment.setStatus(0);
            iCallTaskAssignmentService.save(assignment);
        } catch (Exception e) {
            log.error("任务联系人导入监听器 crowdImport error:{}", e.getMessage(), e);
        }
    }

    private void fileImport(CallTaskContactImportEvent event) {
        try {
            JSONObject customerInfo = event.getCustomerInfo();
            Long customerId = event.getCustomerId();
            String phone = customerInfo.getString("phone");
            // 未匹配档案且勾选"同步创建公海档案"：按导入模板建档（来源=3 任务导入，建为无主公海客户，
            // phone/name 为生成列自动取 customer_info 系统字段，当晚人群重算后即可被圈选）
            if (Objects.isNull(customerId) && Boolean.TRUE.equals(event.getCreateCustomer()) && Objects.nonNull(phone) && !phone.isBlank()) {
                CustomerSeas seas = new CustomerSeas();
                seas.setTemplateId(event.getTemplateId());
                seas.setCustomerInfo(customerInfo.toJSONString());
                seas.setSource(3);
                iCustomerSeasService.save(seas);
                customerId = seas.getId();
                log.info("任务Excel导入同步建档 customerId:{} taskId:{}", customerId, event.getTaskId());
            }
            CallTaskAssignment assignment = new CallTaskAssignment();
            assignment.setTaskId(event.getTaskId());
            assignment.setPhone(phone);
            assignment.setName(customerInfo.getString("name"));
            assignment.setExt(customerInfo.toJSONString());
            assignment.setSource(1);
            assignment.setTemplateId(event.getTemplateId());
            assignment.setCustomerId(customerId);
            assignment.setStatus(0);
            iCallTaskAssignmentService.save(assignment);
        } catch (Exception e) {
            log.error("任务联系人导入监听器 fileImport error:{}", e.getMessage(), e);
        }
    }


    @Override
    public boolean supportsAsyncExecution() {
        return true;
    }
}
