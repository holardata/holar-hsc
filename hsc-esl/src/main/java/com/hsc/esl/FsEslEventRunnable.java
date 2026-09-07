// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl;

import cn.hutool.core.util.IdUtil;
import com.hsc.common.utils.StringUtils;
import com.hsc.common.utils.TraceUtil;
import com.hsc.esl.factory.FsEslEventFactory;
import com.hsc.esl.utils.EslEventUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author danmo
 * @date 2023-10-20 17:19
 **/
@Slf4j
public class FsEslEventRunnable implements Runnable {

    private final FsEslEventFactory factory;

    @Getter
    private final FsEslMsg msg;

    public FsEslEventRunnable(FsEslEventFactory factory, FsEslMsg msg) {
        this.factory = factory;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            String uniqueId = EslEventUtil.getUniqueId(msg.getEslEvent());
            TraceUtil.setTraceId(StringUtils.isEmpty(uniqueId) ? IdUtil.fastSimpleUUID() : uniqueId);
            // 高频 ESL 事件(HEARTBEAT/RE_SCHEDULE 等)DEBUG 日志刷屏干扰排查，暂注释；排查事件流时临时打开
            // log.debug("【接收EslEvent事件消息消费】 {}, {}", msg.getEslEvent().getEventName(), uniqueId);
            factory.getResource(msg.getAddress(), msg.getEslEvent());
        } catch (Exception e) {
            log.error("EslEvent事件消息消费失败 {}, {}", msg.getEslEvent().getEventName(), EslEventUtil.getUniqueId(msg.getEslEvent()),e);
        }finally {
            TraceUtil.clear();
        }
    }
}
