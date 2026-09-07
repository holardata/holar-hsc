// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.HangupCauseEnum;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.fssip.FsSipGatewayQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.sip.FsRegVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@EslRouteName(RouteTypeEnum.AGENT)
@Component
@Slf4j
public class FsAgentRouteHandler extends FsAbstractRouteHandler {

    @Autowired
    private ISipRegService sipRegService;


    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String agentId) {

        Long agentRouteId = parseLongRouteValue(agentId, address, callInfo, uniqueId);
        if (agentRouteId == null) {
            return;
        }
        SipAgentVo sipAgent = iSipAgentService.getDetail(agentRouteId);
        if(Objects.isNull(sipAgent)){
            log.error("转坐席未查询到坐席信息 callId:{}  callerNumber:{} calleeNumber:{},agentId:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee(),agentId);
            fsClient.hangupCall(address,callInfo.getCallId(),uniqueId);
            return;
        }

        String otherUniqueId = RandomUtil.randomNumbers(32);

        //坐席分机号码
        String calleeNumber = sipAgent.getAgentNumber();
        if(StringUtils.isEmpty(calleeNumber)){
            log.error("坐席未配置sip号码 callId:{}  callerNumber:{} calleeNumber:{},agentId:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee(),agentId);
            fsClient.hangupCall(address,callInfo.getCallId(),uniqueId);
            return;
        }
        log.info("转坐席 callId:{}, agent:{}", callInfo.getCallId(), agentId);

        // 坐席分机未注册到 FS（未签入/离线）→ 直接挂断：sofia reg 实时查询最准，
        // 避免单个坐席路由 originate 呼空致主叫一直振铃（Redis onlineStatus 靠前端 WS 推，
        // 异常断开会假在线；sofia reg 反映分机真实注册状态）。
        List<FsRegVo> regList = sipRegService.getList(calleeNumber);
        if (CollectionUtil.isEmpty(regList)) {
            log.info("转坐席 坐席未注册(未签入)挂断 callId:{} agentId:{} agentNumber:{}", callInfo.getCallId(), agentId, calleeNumber);
            callInfo.setSkillHangUpReason(HangupCauseEnum.FULLBUSY.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }

        callInfo.setCallee(calleeNumber);
        callInfo.setAgentId(sipAgent.getId());
        callInfo.setAgentName(sipAgent.getName());
        callInfo.setAgentNumber(sipAgent.getAgentNumber());

        // 1.11 坐席分配后同步更新 CallRecord，保证通话中 ws 推送能定位坐席
        updateCallRecordAgent(callInfo);

        //构建被叫通道
        ChannelInfo otherChannelInfo = ChannelInfo.builder().callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(1).type(1).directionType(2)
                .agentId(sipAgent.getId()).agentNumber(sipAgent.getAgentNumber()).agentName(sipAgent.getName())
                .callTime(DateUtil.current()).otherUniqueId(uniqueId)
                .called(calleeNumber).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.setChannelInfoMap(otherUniqueId,otherChannelInfo);
        bindMutualOtherUniqueId(callInfo, uniqueId, otherUniqueId);
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);


        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(1);

        FsSipGatewayQuery query = new FsSipGatewayQuery();
        query.setGatewayType(0);
        List<FsSipGateway> gatewayList = iFsSipGatewayService.getList(query);
        // 先落缓存再 originate：坐席腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        if(CollectionUtil.isNotEmpty(gatewayList)){
            fsClient.makeCall(address,callInfo.getCallId(), calleeNumber,callInfo.getCallerDisplay(),otherUniqueId,callInfo.getCalleeTimeOut(), gatewayList.get(0));
        }else {
            log.error("转坐席未查询到非外线网关 callId:{}  callerNumber:{} calleeNumber:{},agentId:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee(),agentId);
            fsClient.hangupCall(address,callInfo.getCallId(),uniqueId);
        }
        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
    }
}
