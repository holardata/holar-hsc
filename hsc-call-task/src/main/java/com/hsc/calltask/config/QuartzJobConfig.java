// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.config;


import com.hsc.calltask.job.AbstractRecordJob;
import com.hsc.calltask.job.CustomerCrowdJob;
import org.quartz.*;
import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * 定时任务配置
 *
 * @author danmo
 * @date 2025/7/2 11:26
 */
@Configuration
public class QuartzJobConfig {

    private static final String CUSTOMER_CROWD_GROUP = "CustomerCrowd";
    private static final String CUSTOMER_CROWD_JOB = "CustomerCrowdJob";

    //每天凌晨2点执行
    private static final String CUSTOMER_CROWD_CRON = "0 0 2 * * ?";

    @Bean
    public JobDetail customerCrowdDetail() {
        return JobBuilder.newJob().ofType(CustomerCrowdJob.class)
                .withIdentity(CUSTOMER_CROWD_JOB, CUSTOMER_CROWD_GROUP)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger customerCrowdTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(customerCrowdDetail())
                .withIdentity(CUSTOMER_CROWD_JOB, CUSTOMER_CROWD_GROUP)
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(CUSTOMER_CROWD_CRON))
                .build();

    }

    // ===== 过程摘要定时任务（integrate-hotline-assistant 1.14）=====
    private static final String ABSTRACT_RECORD_GROUP = "AbstractRecord";
    private static final String ABSTRACT_RECORD_JOB = "AbstractRecordJob";
    // 每 2 分钟扫描未处理 ASR 生成过程摘要
    private static final String ABSTRACT_RECORD_CRON = "0 0/2 * * * ?";

    @Bean
    public JobDetail abstractRecordDetail() {
        return JobBuilder.newJob().ofType(AbstractRecordJob.class)
                .withIdentity(ABSTRACT_RECORD_JOB, ABSTRACT_RECORD_GROUP)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger abstractRecordTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(abstractRecordDetail())
                .withIdentity(ABSTRACT_RECORD_JOB, ABSTRACT_RECORD_GROUP)
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(ABSTRACT_RECORD_CRON))
                .build();
    }
}
