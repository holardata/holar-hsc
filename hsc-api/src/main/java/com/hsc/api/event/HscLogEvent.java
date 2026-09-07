package com.hsc.api.event;

import com.hsc.system.domain.entity.SysOperLog;
import org.springframework.context.ApplicationEvent;

/**
 * @author danmo
 * @date 2024-03-12 18:58
 **/
public class HscLogEvent extends ApplicationEvent {

    public HscLogEvent(SysOperLog log) {
        super(log);
    }
}
