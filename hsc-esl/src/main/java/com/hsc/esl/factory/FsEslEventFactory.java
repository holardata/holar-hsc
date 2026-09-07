// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.factory;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@Component
public class FsEslEventFactory {

    /** eventName → handler 预建索引：原实现每条事件线性遍历全部 handler 反射读注解，高频事件下纯浪费 */
    private final Map<String, FsEslEventHandler> handlerIndex = new ConcurrentHashMap<>();

    public FsEslEventFactory(Map<String, FsEslEventHandler> eventMap) {
        for (FsEslEventHandler handler : eventMap.values()) {
            EslEventName handlerName = handler.getClass().getAnnotation(EslEventName.class);
            if (handlerName == null) {
                handlerName = handler.getClass().getSuperclass().getAnnotation(EslEventName.class);
            }
            if (handlerName == null || StringUtils.isEmpty(handlerName.value())) {
                continue;
            }
            handlerIndex.put(handlerName.value(), handler);
        }
    }

    public void getResource(String address, EslEvent event) {
        FsEslEventHandler handler = handlerIndex.get(event.getEventName());
        if (handler != null) {
            handler.handleEslEvent(address, event);
        }
    }
}
