// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@EslRouteName(RouteTypeEnum.SIP)
@Component
@Slf4j
public class FsSipRouteHandler extends FsAbstractRouteHandler {

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String sipValue) {
        log.info("转SIP callId:{} transfer to {}", callInfo.getCallId(), sipValue);
        // route_value 格式为"SIP号码@网关地址"；缺 @ 无法定位出局网关，兜底挂断
        String[] destinationArr = sipValue.split(Constants.AT);
        if (destinationArr.length < 2) {
            log.error("转SIP路由值格式错误(缺少@网关地址) callId:{} sipValue:{}", callInfo.getCallId(), sipValue);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        String otherUniqueId = RandomUtil.randomNumbers(32);
        callInfo.setCallee(destinationArr[0]);
        String gatewayAddress = destinationArr[1];
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
        detail.setTransferType(6);

        // 先落缓存再 originate：B 腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        // called=被叫(SIP目标号)、calledDisplay=主叫显号(与上面 B 腿 display 一致)
        fsClient.makeCall(address, callInfo.getCallId(), callInfo.getCallee(), callInfo.getCallerDisplay(), otherUniqueId, callInfo.getCalleeTimeOut(), gatewayAddress);

        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
    }
}
