// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.date.DateUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.esl.service.IFlowNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@RequiredArgsConstructor
@EslRouteName(RouteTypeEnum.IVR)
@Component
@Slf4j
public class FsIvrRouteHandler extends FsAbstractRouteHandler {

    private final IFlowNoticeService iFlowNoticeService;

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String flowId) {
        log.info("转ivr callId:{} transfer to {}", callInfo.getCallId(), flowId);
        // 脏配置防护(呼入路由/IVR转接/技能组溢出共用本入口)：flowId 非数字(脏数据/绕过
        // 前端提交)直接挂断——原样 parseLong 会抛 NumberFormatException 打断调用线程且主叫腿悬空
        Long flowRouteId = parseLongRouteValue(flowId, address, callInfo, uniqueId);
        if (flowRouteId == null) {
            return;
        }

        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(2);
        detail.setTransferId(flowRouteId);
        iFlowNoticeService.notice(address, callInfo.getCallId(),uniqueId, flowRouteId);
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
    }
}
