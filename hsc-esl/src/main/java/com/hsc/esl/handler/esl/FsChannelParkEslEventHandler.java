// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.AgentStateEnum;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.handler.call.FsCallIRouteProcess;
import com.hsc.esl.utils.EslEventUtil;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.query.display.CallDisplayQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.display.CallDisplayVo;
import com.hsc.system.domain.vo.route.CallRouteVo;
import com.hsc.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.hsc.common.enums.DirectionEnum.INBOUND;
import static com.hsc.common.enums.DirectionEnum.OUTBOUND;

/**
 * 话机振铃
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_PARK)
@Component
public class FsChannelParkEslEventHandler extends AbstractFsEslEventHandler {

    /**
     * 软电话外呼被叫振铃超时(秒)默认值：运行时统一读 sys_config 参数
     * {@code call.ring-timeout.outbound}(参数页可调实时生效)，本常量仅参数缺失时兜底，
     * 与预览式外呼 CallQuery 兜底同源，勿再散落魔法数。
     */
    private static final int OUTBOUND_CALLEE_RING_TIMEOUT = EslConstant.OUTBOUND_CALLEE_RING_TIMEOUT;

    @Autowired
    private ISysConfigService iSysConfigService;

    @Lazy
    @Autowired
    private FsCallIRouteProcess fsCallIRouteProcess;

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        if (EslConstant.OK.equals(EslEventUtil.getSipHangupPhrase(event))) {
            return;
        }
        String uniqueId = EslEventUtil.getUniqueId(event);
        log.info("ChannelParkEslEventHandler uniqueId:{}", uniqueId);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);

        if (callInfo == null && INBOUND.name().equalsIgnoreCase(EslEventUtil.getCallDirection(event))) {
            //外呼入口判定：主叫号绑定了坐席(sip_agent 可查到) → 坐席外呼。不按 User-Agent 判断——
            //座机/其它 SIP 终端(硬话机 UA 各异)手拨外线同样应走外呼链路(补显号出局+话单+录音)；
            //外部来电主叫是公网号码查不到坐席绑定，不会误判。坐席互打不进 PARK(FS 分机直连)
            String callerNumber = EslEventUtil.getCallerCallerIdNumber(event);
            if (Objects.nonNull(iSipAgentService.getInfoByAgent(callerNumber))) {
                outboundCall(address,event);
                return;
            }else {
                inboundCall(address,event);
                return;
            }
        }

        if (Objects.isNull(callInfo) || callInfo.getHangupDir() != null) {
            return;
        }

        ChannelInfo channelInfo = callInfo.getChannelMap().get(uniqueId);
        if (Objects.isNull(channelInfo)) {
            return;
        }
        if (StringUtils.isNotEmpty(EslEventUtil.getSipHangupPhrase(event))) {
            return;
        }
        if (channelInfo.getAnswerTime() != null && channelInfo.getState() != null) {
            return;
        }
        channelInfo.setRingStartTime(event.getEventDateTimestamp() / 1000);
        if(Objects.equals(1,channelInfo.getDirectionType())){
            callInfo.setCallerRingStartTime(channelInfo.getRingStartTime());
        }
        if(Objects.equals(2,channelInfo.getDirectionType())){
            callInfo.setCalleeRingStartTime(channelInfo.getRingStartTime());
        }

        DirectionEnum directionEnum = DirectionEnum.getByType(callInfo.getDirection());
        if (directionEnum != null){
            switch (directionEnum){
                case INBOUND -> {
                    sendAgentStatus(callInfo.getCallId(),callInfo.getCaller(),callInfo.getCallee(),INBOUND.getType(), AgentStateEnum.CALL_OUT_RING);
                }
                case OUTBOUND -> {
                    sendAgentStatus(callInfo.getCallId(),callInfo.getCaller(),callInfo.getCallee(), OUTBOUND.getType(), AgentStateEnum.CALL_INT_RING);
                }
                default -> {
                }
            }
        }
        log.info("ChannelParkEslEventHandler after callId:{}, caller:{}, called:{}, uniqueId:{}", callInfo.getCallId(),callInfo.getCaller(), callInfo.getCallee(), uniqueId);
        callInfo.setChannelInfoMap(uniqueId,channelInfo);
        ifsCallCacheService.saveCallInfo(callInfo);

    }

    /**
     * 呼入
     * @param event
     */
    private void inboundCall(String address, EslEvent event) {
        Long callId = IdUtil.getSnowflakeNextId();
        String uniqueId = EslEventUtil.getUniqueId(event);
        String callerNumber = EslEventUtil.getCallerCallerIdNumber(event);
        String calleeNumber = EslEventUtil.getCallerDestinationNumber(event);
        String sipContactUri = EslEventUtil.getSipContactUri(event);
        log.info("park >>>>>>>inbound callId:{}, caller:{}, called:{}, uniqueId:{}, sipContactUri:{}", callId,callerNumber, calleeNumber, uniqueId, sipContactUri);
        String location = iPhoneLocationService.getLocation(callerNumber);
        //构建呼叫总线
        // calleeTimeOut 统一读 sys_config 参数 call.ring-timeout.inbound-agent(参数页可调实时生效，
        // 默认 EslConstant.INBOUND_AGENT_RING_TIMEOUT)：坐席/技能组轮呼/SIP/AI转人工的 B 腿 originate
        // 都经 callInfo.getCalleeTimeOut() 消费；座机路由由 FsExtensionRouteHandler 单独读座机专属参数
        CallInfo callInfo = CallInfo.builder().callId(callId)
                .caller(callerNumber).calleeDisplay(calleeNumber).direction(INBOUND.getType())
                .calleeTimeOut(iSysConfigService.getInt(SysConfigKeys.RING_TIMEOUT_INBOUND_AGENT, EslConstant.INBOUND_AGENT_RING_TIMEOUT))
                .callTime(DateUtil.current()).callerDisplay(callerNumber).numberLocation(location).routeType(2).build();
        callInfo.addUniqueIdList(uniqueId);
        //构建主叫通道
        ChannelInfo channelInfo = ChannelInfo.builder().callId(callId).uniqueId(uniqueId).cdrType(1).type(2).directionType(1).callTime(DateUtil.current())
                .caller(callerNumber).called(calleeNumber).display(calleeNumber).build();
        callInfo.setChannelInfoMap(uniqueId,channelInfo);


        // 早应答改造：AI/IVR 路由 answer（AI 对话、IVR 放音收键都要 media）；人工路由不 answer（坐席接听才 answer，手机不秒接通）
        boolean earlyAnswerRoute = isEarlyAnswerRoute(callInfo);
        callInfo.setProcess(earlyAnswerRoute ? ProcessEnum.CALL_ROUTE : ProcessEnum.CALL_BRIDGE);

        ifsCallCacheService.saveCallInfo(callInfo);
        ifsCallCacheService.saveCallRel(uniqueId,callId);

        // 1.11 同步落 CallRecord，保证后续 ai-callbot 旁路 ASR 文本到达时能按 callId 关联
        saveInitialCallRecord(callInfo);

        log.info("[早应答] 分支 callId:{} earlyAnswerRoute:{} 主叫腿:{}", callInfo.getCallId(), earlyAnswerRoute, uniqueId);
        if (earlyAnswerRoute) {
            // AI/IVR 路由：answer 主叫 → CHANNEL_ANSWER → CALL_ROUTE process（录音 + executeRoute → FsAi/FsIvrRouteHandler）
            log.info("[早应答] AI/IVR路由 → answer主叫 callId:{} 主叫腿:{}", callInfo.getCallId(), uniqueId);
            fsClient.answer(address, uniqueId);
        } else {
            // 人工路由：跳过 CALL_ROUTE（不经主叫 answer），直接 executeRoute originate 坐席；
            // 坐席接听 → CHANNEL_ANSWER → CALL_BRIDGE process → answer 主叫 + bridge
            log.info("[早应答] 人工路由 → 不answer,executeRoute originate坐席 callId:{} 主叫腿:{}", callInfo.getCallId(), uniqueId);
            fsCallIRouteProcess.executeRoute(address, callInfo, uniqueId);
        }
    }

    /**
     * 是否需要早应答的路由（AI=8 对话要 media；IVR=6 放音/收键要 media）。
     * 人工类路由（坐席/外呼/SIP/技能组/座机）延迟到坐席接听才 answer。
     */
    private boolean isEarlyAnswerRoute(CallInfo callInfo) {
        Integer routeType = getInboundRouteType(callInfo);
        log.info("[早应答] 查呼入路由 callId:{} callee:{} direction:{} routeType:{}(6=IVR,8=AI)",
                callInfo.getCallId(), callInfo.getCallee(), callInfo.getDirection(), routeType);
        return Objects.equals(routeType, RouteTypeEnum.AI.getType())
                || Objects.equals(routeType, RouteTypeEnum.IVR.getType());
    }

    /** 查呼入路由类型（无路由返回 null） */
    private Integer getInboundRouteType(CallInfo callInfo) {
        CallRouteVo callRoute = ifsCallCacheService.getCallRoute(callInfo.getCallee(), callInfo.getCaller(), callInfo.getDirection());
        return callRoute == null ? null : callRoute.getRouteType();
    }

    /**
     * 呼出
     * @param address
     * @param event
     */
    private void outboundCall(String address,EslEvent event) {
        String callerNumber = EslEventUtil.getCallerCallerIdNumber(event);
        String calleeNumber = EslEventUtil.getCallerDestinationNumber(event);
        String uniqueId = EslEventUtil.getUniqueId(event);
        Long callId = IdUtil.getSnowflakeNextId();
        SipAgentVo sipAgent = iSipAgentService.getInfoByAgent(callerNumber);
        if(Objects.isNull(sipAgent)){
            log.error("软电话外呼未查询到坐席信息 callId:{}  callerNumber:{} calleeNumber:{}", callId, callerNumber, calleeNumber);
            fsClient.hangupCall(address,callId,uniqueId);
            return;
        }
        // 归属地查被叫(客户)号码——外呼场景主叫是坐席分机号查不出归属地(对照呼入查主叫=客户)
        String location = iPhoneLocationService.getLocation(calleeNumber);
        //构建呼叫总线
        // 任务/私海拨打身份标识（软电话自定义 X 头透传，任务拨打带三值、私海拨打仅带客户、
        // 普通手拨/呼入为 null 零侵入）；挂断收尾据此发 TaskDialFinishEvent（不写话单，关联由拨打历史持有）
        CallInfo callInfo = CallInfo.builder().callId(callId)
                .agentId(sipAgent.getId()).agentNumber(sipAgent.getAgentNumber()).agentName(sipAgent.getName())
                .assignmentId(EslEventUtil.getSipHAssignmentId(event))
                .taskId(EslEventUtil.getSipHTaskId(event))
                .customerId(EslEventUtil.getSipHCustomerId(event))
                .caller(callerNumber).callee(calleeNumber).numberLocation(location).direction(OUTBOUND.getType())
                .calleeTimeOut(iSysConfigService.getInt(SysConfigKeys.RING_TIMEOUT_OUTBOUND, OUTBOUND_CALLEE_RING_TIMEOUT))
                .callTime(DateUtil.current()).build();
        callInfo.addUniqueIdList(uniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);

        //获取外呼显号
        CallDisplayQuery displayQuery = new CallDisplayQuery();
        List<CallDisplayVo> displayList = iCallDisplayService.getList(displayQuery);
        if(CollectionUtil.isEmpty(displayList)){
            log.error("电话外呼未查询到显号信息 callId:{}  callerNumber:{} calleeNumber:{}", callId, callerNumber, calleeNumber);
            fsClient.hangupCall(address,callId,uniqueId);
            return;
        }

        CallDisplayVo displaySimple = RandomUtil.randomEle(displayList);
        callInfo.setCallerDisplay(sipAgent.getAgentNumber());
        callInfo.setCalleeDisplay(displaySimple.getPhone());


        //构建主叫通道(主叫=坐席,被叫=客户号码;与 inboundCall 主叫腿方向一致)
        ChannelInfo channelInfo = ChannelInfo.builder().callId(callId).uniqueId(uniqueId).cdrType(2).type(1).directionType(1)
                .agentId(sipAgent.getId()).agentNumber(sipAgent.getAgentNumber()).agentName(sipAgent.getName())
                .callTime(DateUtil.current())
                .caller(callerNumber).called(calleeNumber).display(callInfo.getCalleeDisplay()).build();
        callInfo.setChannelInfoMap(uniqueId,channelInfo);

        ifsCallCacheService.saveCallInfo(callInfo);
        ifsCallCacheService.saveCallRel(uniqueId,callId);

        // 1.11 同步落 CallRecord
        saveInitialCallRecord(callInfo);

        // 晚应答：不预先 answer 主叫腿，直接 originate 被叫；主叫保持 ringing，软电话播回铃音，
        // 被叫接听后由 FsCallBridgeProcess answer 主叫 + bridge（同 inboundCall 人工路由模式）。
        fsCallIRouteProcess.executeRoute(address, callInfo, uniqueId);
    }

    /**
     * 1.11 在 CallInfo 创建(PARK)时同步落 CallRecord，保证后续 ASR 文本能按 callId 关联。
     * 呼入场景坐席尚未分配，agentId/Number/Name 填兜底，后续由挂断时 transfer 更新补全。
     * 注：spec D3 写"CHANNEL_CREATE 建"，但 hsc callId(雪花ID)在 PARK 才生成，故落地在 PARK（已确认）。
     */
    private void saveInitialCallRecord(CallInfo callInfo) {
        CallRecord record = new CallRecord();
        record.setCallId(String.valueOf(callInfo.getCallId()));
        record.setCallerNumber(callInfo.getCaller());
        record.setCalleeNumber(callInfo.getCallee() != null ? callInfo.getCallee() : callInfo.getCalleeDisplay());
        record.setCallerDisplayNumber(callInfo.getCallerDisplay());
        record.setCalleeDisplayNumber(callInfo.getCalleeDisplay());
        record.setNumberLocation(callInfo.getNumberLocation());
        record.setAgentId(callInfo.getAgentId() == null ? 0L : callInfo.getAgentId());
        record.setAgentNumber(callInfo.getAgentNumber() == null ? "" : callInfo.getAgentNumber());
        record.setAgentName(callInfo.getAgentName() == null ? "" : callInfo.getAgentName());
        record.setDirection(CallRecord.mapDirectionFromCallInfo(callInfo.getDirection()));
        record.setCallStartTime(new Date());
        record.setAnswerFlag(0);
        record.setCallState(1);
        iCallRecordService.save(record);
    }

}
