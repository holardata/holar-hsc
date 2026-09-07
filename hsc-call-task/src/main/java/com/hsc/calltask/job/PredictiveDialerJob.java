// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.job;

import com.hsc.calltask.domain.entity.CallTask;
import com.hsc.calltask.handler.CallTaskHandler;
import com.hsc.calltask.service.ICallTaskService;
import com.hsc.common.enums.CallTaskStatusEnum;
import com.hsc.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * 外呼任务
 *
 * @author danmo
 * @date 2025/06/25
 */
@RequiredArgsConstructor
@Slf4j
@Component
@DisallowConcurrentExecution
public class PredictiveDialerJob extends QuartzJobBean {

    private final ICallTaskService callTaskService;

    private final Map<String, CallTaskHandler> callTaskHandlerMap;


    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String taskId = context.getJobDetail().getJobDataMap().getString("taskId");
        log.info("开始执行预测外呼任务,任务ID：{}", taskId);
        try {
            CallTask callTask = callTaskService.getById(taskId);
            if (Objects.isNull(callTask)) {
                log.info("任务不存在,任务ID：{}", taskId);
                return;
            }
            if (CallTaskStatusEnum.NOT_START.getCode().equals(callTask.getStatus())) {
                callTaskService.updateStatus(Long.valueOf(taskId), CallTaskStatusEnum.PROCESSING);
            }
            if (CallTaskStatusEnum.END.getCode().equals(callTask.getStatus())) {
                log.info("任务已结束,任务ID：{}", taskId);
                return;
            }
            if (CallTaskStatusEnum.PAUSE.getCode().equals(callTask.getStatus())) {
                log.info("任务已暂停,任务ID：{}", taskId);
                return;
            }
            // 人工外呼唯一形态为预览式（预测式/AI 外呼专项暂缓），直接取预览分配处理器
            CallTaskHandler callTaskHandler = callTaskHandlerMap.get("previewOutboundHandler");
            callTaskHandler.execute(Long.valueOf(taskId));
        } catch (Exception e) {
            log.error("任务执行异常 taskId:{}", taskId, e);
            throw new JobExecutionException("任务执行异常 taskId:" + taskId, e);
        } finally {
            Date nextFireTime = context.getNextFireTime();
            if (Objects.isNull(nextFireTime) && StringUtils.isNotBlank(taskId)) {
                log.info("任务已结束,任务ID：{}", taskId);
                callTaskService.updateStatus(Long.valueOf(taskId), CallTaskStatusEnum.END);
            }
        }

    }

}
