package com.hsc.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 技能组坐席腿未接通挂断(漏接)事件：FsChannelHangUpCompleteEslEventHandler 发出、
 * 两个技能组 handler 各自监听并按链路归属响应（flowDataContext 有无区分 IVR 链路）——
 * 重调技能组 handler 重分配（本通 1 次上限、排除漏接坐席），超限走溢出策略。
 * 同步事件，在 ESL 事件线程内执行。
 */
@Getter
public class SkillAgentMissedEvent extends ApplicationEvent {

    private final Long callId;

    /** 主叫腿 uniqueId（重分配入口用它做号码路由查询/转接） */
    private final String callerUniqueId;

    /** FS 实例地址 */
    private final String address;

    public SkillAgentMissedEvent(Object source, Long callId, String address, String callerUniqueId) {
        super(source);
        this.callId = callId;
        this.address = address;
        this.callerUniqueId = callerUniqueId;
    }
}
