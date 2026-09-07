package com.hsc.esl.handler.route;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.fssip.FsSipGatewayQuery;
import com.hsc.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 座机/分机路由：route_value 直接是分机号(如 1412)，后端 originate user/分机@realm
 * 拨打 FS 注册表里注册的硬件座机。
 *
 * <p>⚠️ 不查 sip_agent 表：防止有人用软电话签入同号把座机踢掉(SIP 同号互踢)的防护已上移到
 * sipConfig 层(座机坐席不下发 SIP 密码)。agentNumber 仍写入 CallRecord，用于通话中
 * ws 推送(转写/推荐/AI 工作台)按 agentNumber→sip_agent.userId 定位座机坐席浏览器。
 * 媒体协商由 FsClient.makeCall 按 ko_subscriber.terminal_type 自动区分(座机不带 media_webrtc=true)。
 *
 * @author pangshuai
 * @date 2026-07-31
 **/
@EslRouteName(RouteTypeEnum.EXTENSION)
@Component
@Slf4j
public class FsExtensionRouteHandler extends FsAbstractRouteHandler {

    @Autowired
    private ISysConfigService iSysConfigService;

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String extensionNumber) {
        if (StringUtils.isEmpty(extensionNumber)) {
            log.error("转座机分机号为空 callId:{}", callInfo.getCallId());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        log.info("转座机 callId:{}, extension:{}", callInfo.getCallId(), extensionNumber);

        String otherUniqueId = RandomUtil.randomNumbers(32);
        callInfo.setCallee(extensionNumber);
        // 座机写 agentNumber（仅号码字符串，不查 sip_agent 关联 agentId/agentName）：
        // 保证 bridge 后 forkAudioToAsrBot 带 real agentNumber、ws 推送定位能按
        // agentNumber→sip_agent.userId 找到座机坐席的浏览器
        callInfo.setAgentNumber(extensionNumber);
        updateCallRecordAgent(callInfo);

        //构建被叫通道(cdrType/type/directionType 同 FsAgentRouteHandler 转注册分机路径)
        ChannelInfo otherChannelInfo = ChannelInfo.builder()
                .callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(1).type(1).directionType(2)
                .callTime(DateUtil.current()).otherUniqueId(uniqueId)
                .called(extensionNumber).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.setChannelInfoMap(otherUniqueId, otherChannelInfo);
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);

        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(7);

        // 取 internal 网关(gatewayType=0)，FsClient.makeCall 据此拼成 user/分机@realm 直拨注册分机
        FsSipGatewayQuery query = new FsSipGatewayQuery();
        query.setGatewayType(0);
        List<FsSipGateway> gatewayList = iFsSipGatewayService.getList(query);
        // 先落缓存再 originate：座机腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        if (CollectionUtil.isNotEmpty(gatewayList)) {
            // 座机振铃超时读座机专属参数(参数页可调实时生效,默认 EslConstant.INBOUND_EXTENSION_RING_TIMEOUT)：
            // 单独传 makeCall 形参、不改共享 callInfo.calleeTimeOut——后者是呼入坐席超时
            // (inbound-agent)，技能组轮呼等场景仍按它消费，勿互相污染
            fsClient.makeCall(address, callInfo.getCallId(), extensionNumber, callInfo.getCallerDisplay(),
                    otherUniqueId,
                    iSysConfigService.getInt(SysConfigKeys.RING_TIMEOUT_INBOUND_EXTENSION, EslConstant.INBOUND_EXTENSION_RING_TIMEOUT),
                    gatewayList.get(0));
        } else {
            log.error("转座机未查询到 internal 网关 callId:{} extension:{}", callInfo.getCallId(), extensionNumber);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
        }

        detail.setEndTime(DateUtil.current());
        callInfo.addDetailList(detail);
        fsCallCacheService.saveCallInfo(callInfo);
    }
}
