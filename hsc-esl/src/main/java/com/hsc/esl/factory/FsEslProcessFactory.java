// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.factory;

import com.hsc.common.annotation.EslProcessName;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.esl.handler.call.FsAbstractCallProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@Component
public class FsEslProcessFactory {

    private final Map<String, FsAbstractCallProcess> callProcessMap = new ConcurrentHashMap<>(16);

    public FsEslProcessFactory(Map<String, FsAbstractCallProcess> callProcessMap) {
        this.callProcessMap.putAll(callProcessMap);
    }

    public FsAbstractCallProcess factory(ProcessEnum processEnum) {
        for (FsAbstractCallProcess handler : callProcessMap.values()) {
            EslProcessName processName = handler.getClass().getAnnotation(EslProcessName.class);
            if (processName == null) {
                processName = handler.getClass().getSuperclass().getAnnotation(EslProcessName.class);
            }
            if (processName == null) {
                continue;
            }
            if (Objects.equals(processEnum, processName.value())) {
                return handler;
            }
        }
        return null;
    }

}
