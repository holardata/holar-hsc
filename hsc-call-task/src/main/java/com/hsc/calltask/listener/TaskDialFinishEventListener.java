package com.hsc.calltask.listener;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.calltask.domain.entity.CallTaskAssignment;
import com.hsc.calltask.domain.entity.CallTaskDialLog;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.service.ICallTaskAssignmentService;
import com.hsc.calltask.service.ICallTaskDialLogService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.enums.CallResultEnum;
import com.hsc.common.event.TaskDialFinishEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 外呼拨打完成监听器：TaskDialFinishEvent 的消费者（hsc-esl 挂断收尾依据 CallInfo 身份标识发布，模块解耦）。
 *
 * <p>坐席发起的任务/私海通话（软电话 SIP 头携带或座机代拨入参携带身份）挂断后回写三处：
 * ① 拨打历史 call_task_dial_log 落一条（结果/时长/话单关联，任务与私海统一）；
 * ② 任务联系人聚合回写（assignmentId 非空时）：call_status=1 + call_result + last_dial_time +
 *    last_talk_duration + attempt_count 原子+1（仅统计真实发生并挂断的呼叫，点拨未呼出不计）；
 * ③ 客户外呼痕迹（customerId 非空时）：last_dial_time + dial_count 原子+1（无档案跳过）。
 *
 * <p>结果判定走 {@link CallResultEnum#of(boolean, Integer)} 唯一入口（事件带 answered+causeCode 原料）。
 * 异步执行(@Async)不阻塞 ESL 事件线程；回写失败仅记日志不重试（fs_cdr 可人工对账）。
 *
 * @author danmo
 * @date 2026/8/27
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class TaskDialFinishEventListener {

    private final ICallTaskAssignmentService iCallTaskAssignmentService;
    private final ICallTaskDialLogService iCallTaskDialLogService;
    private final ICustomerSeasService iCustomerSeasService;

    @Async("asyncTaskExecutor")
    @EventListener
    public void onDialFinish(TaskDialFinishEvent event) {
        try {
            CallResultEnum result = CallResultEnum.of(event.isAnswered(), event.getHangupCauseCode());

            // ① 拨打历史（一行=一次真实呼叫；任务拨打带 taskId/assignmentId，私海拨打仅 customerId）
            CallTaskDialLog dialLog = new CallTaskDialLog();
            dialLog.setTaskId(event.getTaskId());
            dialLog.setAssignmentId(event.getAssignmentId());
            dialLog.setAgentId(event.getAgentId());
            dialLog.setCustomerId(event.getCustomerId());
            dialLog.setPhone(event.getPhone());
            dialLog.setCallRecordId(event.getCallRecordId());
            dialLog.setCallId(event.getCallId());
            dialLog.setCallResult(result.getCode());
            dialLog.setHangupCauseCode(event.getHangupCauseCode());
            dialLog.setHangupCause(event.getHangupCause());
            dialLog.setDialTime(event.getDialStartTime());
            dialLog.setTalkDuration(event.getTalkDuration());
            iCallTaskDialLogService.save(dialLog);

            // ② 任务联系人聚合回写（"最近结果"语义：重复拨打覆盖最新值）
            if (event.getAssignmentId() != null) {
                LambdaUpdateWrapper<CallTaskAssignment> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(CallTaskAssignment::getId, event.getAssignmentId())
                        .set(CallTaskAssignment::getCallStatus, 1)
                        .set(CallTaskAssignment::getCallResult, result.getCode())
                        .set(CallTaskAssignment::getLastDialTime, event.getHangupTime())
                        .set(CallTaskAssignment::getLastTalkDuration, event.getTalkDuration())
                        .setSql("attempt_count = IFNULL(attempt_count, 0) + 1");
                iCallTaskAssignmentService.update(wrapper);
            }

            // ③ 客户外呼痕迹（无档案/私海拨打均按 customerId 生效）
            if (event.getCustomerId() != null) {
                LambdaUpdateWrapper<CustomerSeas> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(CustomerSeas::getId, event.getCustomerId())
                        .set(CustomerSeas::getLastDialTime, event.getHangupTime())
                        .set(CustomerSeas::getLastDialResult, result.getCode())
                        .setSql("dial_count = dial_count + 1");
                iCustomerSeasService.update(wrapper);
            }

            log.info("外呼拨打回写完成 assignmentId:{} customerId:{} result:{}(第{}类) talkDuration:{}s hangupTime:{}",
                    event.getAssignmentId(), event.getCustomerId(), result.getMessage(), result.getCode(),
                    event.getTalkDuration(), event.getHangupTime());
        } catch (Exception e) {
            log.error("外呼拨打回写失败 assignmentId:{} customerId:{} callId:{}",
                    event.getAssignmentId(), event.getCustomerId(), event.getCallId(), e);
        }
    }
}
