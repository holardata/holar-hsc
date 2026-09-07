package com.hsc.common.event;

import org.springframework.context.ApplicationEvent;

import java.util.Date;

/**
 * 外呼拨打完成事件：坐席发起的任务/私海通话（软电话直呼或座机代拨）挂断收尾时，
 * 由 ESL HANGUP_COMPLETE handler 依据 CallInfo 持有的身份标识（SIP 头透传/代拨入参携带）发布，
 * hsc-call-task 的 Listener 消费回写：assignment 最近结果列 + 拨打历史 + 客户外呼痕迹。
 *
 * <p>设计说明：hsc-esl 不能依赖 hsc-call-task（模块循环），挂断回写走 Spring 事件解耦，
 * 与 {@link CallHangupEvent} 同款模式；呼叫无身份标识（手动拨号/呼入等）不发布本事件，零侵入。
 *
 * <p>判定结果不直接携带枚举，带 answered+hangupCauseCode 原料，由消费方调
 * {@code CallResultEnum.of(answered, hangupCauseCode)} 唯一入口判定（将来 AI 外呼专项复用同一映射）。
 */
public class TaskDialFinishEvent extends ApplicationEvent {

    /** 任务联系人记录（call_task_assignment.id），私海维度拨打为空 */
    private final Long assignmentId;

    /** 任务ID（call_task.id），私海维度拨打为空 */
    private final Long taskId;

    /** 客户ID（customer_seas.id），无档案为空 */
    private final Long customerId;

    /** 拨打坐席（sip_agent.id） */
    private final Long agentId;

    /** 被叫号码快照（拨打历史记录用） */
    private final String phone;

    /** 话单ID（call_record.id），挂断收尾时话单已生成 */
    private final Long callRecordId;

    /** 雪花 callId（冗余，直查话单用） */
    private final String callId;

    /** 是否接通（answerTime 非空，即 CHANNEL_BRIDGE 过） */
    private final boolean answered;

    /** FS 挂机原因码（FsHangupCauseEnum.code） */
    private final Integer hangupCauseCode;

    /** FS 挂机原因原始串（排障用） */
    private final String hangupCause;

    /** 呼叫发起时间（话单 callStartTime） */
    private final Date dialStartTime;

    /** 通话时长（秒，未接通为 0） */
    private final int talkDuration;

    /** 挂断时间 */
    private final Date hangupTime;

    public TaskDialFinishEvent(Object source, Long assignmentId, Long taskId, Long customerId,
                               Long agentId, String phone, Long callRecordId, String callId, boolean answered,
                               Integer hangupCauseCode, String hangupCause,
                               Date dialStartTime, int talkDuration, Date hangupTime) {
        super(source);
        this.assignmentId = assignmentId;
        this.taskId = taskId;
        this.customerId = customerId;
        this.agentId = agentId;
        this.phone = phone;
        this.callRecordId = callRecordId;
        this.callId = callId;
        this.answered = answered;
        this.hangupCauseCode = hangupCauseCode;
        this.hangupCause = hangupCause;
        this.dialStartTime = dialStartTime;
        this.talkDuration = talkDuration;
        this.hangupTime = hangupTime;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getPhone() {
        return phone;
    }

    public Long getCallRecordId() {
        return callRecordId;
    }

    public String getCallId() {
        return callId;
    }

    public boolean isAnswered() {
        return answered;
    }

    public Integer getHangupCauseCode() {
        return hangupCauseCode;
    }

    public String getHangupCause() {
        return hangupCause;
    }

    public Date getDialStartTime() {
        return dialStartTime;
    }

    public int getTalkDuration() {
        return talkDuration;
    }

    public Date getHangupTime() {
        return hangupTime;
    }
}
