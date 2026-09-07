package com.hsc.calltask.job;

import com.hsc.ai.constant.PromptTitle;
import com.hsc.ai.service.ISummaryService;
import com.hsc.common.event.AbstractUpdatedEvent;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.service.IAbstractRecordService;
import com.hsc.system.service.IDialogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 过程摘要定时任务（call-ai-summary，1.14）。
 *
 * <p>cron 默认每 2 分钟：扫描有未处理(status=0)ASR 句子的 callId，对每个 callId 取未处理句子拼文本，
 * 经 {@link ISummaryService}(hsc-ai 统一网关)生成过程摘要标题+内容，写入 abstract_record，
 * 标记这些句子已处理(status=1)，并发 {@link AbstractUpdatedEvent} 由 hsc-websocket 推 ABSTRACT 给坐席。
 *
 * <p>平迁 rb AbstractTask，适配 hsc Quartz 集群（@DisallowConcurrentExecution 防并发）。
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class AbstractRecordJob extends QuartzJobBean {

    @Autowired
    private IDialogRecordService dialogRecordService;
    @Autowired
    private IAbstractRecordService abstractRecordService;
    @Autowired
    private ISummaryService summaryService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        List<String> callIds = dialogRecordService.listCallIdsWithPendingAsr();
        if (callIds == null || callIds.isEmpty()) {
            return;
        }
        log.info("过程摘要定时任务执行，待处理 callId 数：{}", callIds.size());
        for (String callId : callIds) {
            try {
                processCall(callId);
            } catch (Exception e) {
                log.error("过程摘要生成异常 callId={}", callId, e);
            }
        }
    }

    private void processCall(String callId) {
        List<DialogRecord> dialogs = dialogRecordService.listPendingAsrByCallId(callId);
        if (dialogs == null || dialogs.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (DialogRecord d : dialogs) {
            text.append(d.getChannelType()).append("：").append(d.getDialogTxt()).append("\n");
        }
        String dialogText = text.toString();
        String title = summaryService.generate(callId, PromptTitle.PROCESS_SUMMARY_TITLE, dialogText);
        String content = summaryService.generate(callId, PromptTitle.PROCESS_SUMMARY_CONTENT, dialogText);

        AbstractRecord record = new AbstractRecord();
        record.setCallId(callId);
        record.setDialogTxt(dialogText);
        record.setTitle(title);
        record.setContent(content);
        abstractRecordService.save(record);

        dialogRecordService.markProcessed(callId);
        eventPublisher.publishEvent(new AbstractUpdatedEvent(this, callId, null, title, content));
    }
}
