// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.system.domain.entity.FsSipGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@EslRouteName(RouteTypeEnum.CALLOUT)
@Component
@Slf4j
public class FsCallOutRouteHandler extends FsAbstractRouteHandler {

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String sipGatewayId) {
        log.info("转外呼 callId:{} transfer to {}", callInfo.getCallId(), sipGatewayId);
        String otherUniqueId = RandomUtil.randomNumbers(32);

        //构建被叫通道
        ChannelInfo otherChannelInfo = ChannelInfo.builder().callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(2).type(2).directionType(2)
                .callTime(DateUtil.current()).otherUniqueId(uniqueId)
                .called(callInfo.getCallee()).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setChannelInfoMap(otherUniqueId,otherChannelInfo);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);
        bindMutualOtherUniqueId(callInfo, uniqueId, otherUniqueId);

        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(5);

        Long gatewayId = parseLongRouteValue(sipGatewayId, address, callInfo, uniqueId);
        if (gatewayId == null) {
            return;
        }
        FsSipGateway sipGateway = iFsSipGatewayService.getDetail(gatewayId);
        if (Objects.isNull(sipGateway)) {
            // 路由指向的网关已被删（孤儿路由）：不挂断会在 makeCall 内 NPE
            log.error("转外呼未查询到网关(路由孤儿) callId:{} sipGatewayId:{}", callInfo.getCallId(), sipGatewayId);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        // 先落缓存再 originate：B 腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        fsClient.makeCall(address,callInfo.getCallId(), callInfo.getCallee(),callInfo.getCalleeDisplay(),otherUniqueId,callInfo.getCalleeTimeOut(), sipGateway);
        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
    }
}
