// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain;


import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 任务联系人导入事件
 *
 * @author danmo
 * @date 2025/7/2 22:12
 */
@Getter
public class CallTaskContactImportEvent extends ApplicationEvent {

    /**
     * 客户ID
     */
    private final Long customerId;
    /**
     * 导入方式 0-人群导入 1-文件导入
     */
    private final Integer type;
    /**
     * 任务ID
     */
    private final Long taskId;

    /**
     * 人群ID
     */
    private final Long crowdId;

    /**
     * 模板ID
     */
    private final Long templateId;

    /**
     * 客户信息
     */
    private final JSONObject customerInfo;

    /**
     * Excel 未匹配档案时是否同步创建公海档案（文件导入预检段决定）
     */
    private final Boolean createCustomer;


    public CallTaskContactImportEvent(Long customerId, Long taskId, Integer type, Long crowdId, Long templateId, JSONObject customerInfo) {
        this(customerId, taskId, type, crowdId, templateId, customerInfo, null);
    }

    public CallTaskContactImportEvent(Long customerId, Long taskId, Integer type, Long crowdId, Long templateId, JSONObject customerInfo, Boolean createCustomer) {
        super(taskId);
        this.customerId = customerId;
        this.taskId = taskId;
        this.type = type;
        this.crowdId = crowdId;
        this.templateId = templateId;
        this.customerInfo = customerInfo;
        this.createCustomer = createCustomer;
    }
}
