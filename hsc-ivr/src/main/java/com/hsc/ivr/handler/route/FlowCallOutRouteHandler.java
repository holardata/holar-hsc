// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.route;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.properties.FlowTransferNodeProperties;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.service.IFsSipGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2024-12-31 17:20
 **/
@RequiredArgsConstructor
@Component
@Slf4j
public class FlowCallOutRouteHandler {

    private final IFsCallCacheService fsCallCacheService;
    private final IFsSipGatewayService iFsSipGatewayService;
    private final FsClient fsClient;
    protected final IFlowNoticeService iFlowNoticeService;


    public void handler(FlowDataContext flowData, FlowTransferNodeProperties properties) {
        String sipGatewayId = properties.getRouteValue();
        log.info("转外呼 callId:{} transfer to {}", flowData.getCallId(), sipGatewayId);

        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());

        String otherUniqueId = RandomUtil.randomNumbers(32);

        //构建被叫通道
        ChannelInfo otherChannelInfo = ChannelInfo.builder().callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(2).type(3)
                .callTime(DateUtil.current()).otherUniqueId(flowData.getUniqueId())
                .called(callInfo.getCallee()).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setChannelInfoMap(otherUniqueId,otherChannelInfo);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);

        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(5);

        FsSipGateway sipGateway = iFsSipGatewayService.getDetail(Long.valueOf(sipGatewayId));
        fsClient.makeCall(flowData.getAddress(),callInfo.getCallId(), callInfo.getCaller(),callInfo.getCallee(),otherUniqueId,callInfo.getCalleeTimeOut(), sipGateway);
        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId,callInfo.getCallId());
        //iFlowNoticeService.notice(2, "next", flowData);
    }
}
