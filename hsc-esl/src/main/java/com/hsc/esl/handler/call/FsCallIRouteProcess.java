// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.call;

import cn.hutool.core.date.DateUtil;
import com.hsc.common.annotation.EslProcessName;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.esl.handler.route.FsAbstractRouteHandler;
import com.hsc.system.domain.entity.CallSchedule;
import com.hsc.system.domain.vo.route.CallRouteVo;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 路由
 *
 * @author danmo
 * @date 2023-10-23 17:05
 **/
@EslProcessName(ProcessEnum.CALL_ROUTE)
@Component
@Slf4j
public class FsCallIRouteProcess extends FsAbstractCallProcess {

    /** 座机代拨坐席腿回铃音：FS 内置铃流 tone_stream（2s 响 4s 停，440+480Hz 标准回铃） */
    private static final String RINGBACK_TONE = "tone_stream://%(2000,4000,440,480)";

    @Override
    public void handler(String address, EslEvent event, CallInfo callInfo, ChannelInfo lfsChannelInfo) {
        log.info("进入callRoute电话: callId:{} caller:{} called:{} uniqueId:{}, otherUniqueId:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee(), lfsChannelInfo.getUniqueId(), lfsChannelInfo.getOtherUniqueId());
        lfsCallCacheService.saveCallInfo(callInfo);
        lfsCallCacheService.saveCallRel(lfsChannelInfo.getUniqueId(), callInfo.getCallId());
        // AI 路由：主叫已 answer，启动全程录音（主叫腿贯穿 AI 段 + 转人工后的人工段）
        String filePath = sysSettingConfig.getFsProfile() + "/" + DateUtil.today() + "/" + callInfo.getCallId() + "_" + DateUtil.current() + sysSettingConfig.getFsFileSuffix();
        //设置振铃录音
        fsClient.record(address, callInfo.getCallId(), lfsChannelInfo.getUniqueId(), filePath);
        callInfo.setRecord(filePath);
        callInfo.setRecordStartTime(lfsChannelInfo.getAnswerTime());
        // 早应答路由（AI/IVR，呼入方向）主叫已 answer 即视为接通：补 callInfo.answerTime（否则纯
        // IVR 通话无 bridge 事件，话单 answerFlag 误记"用户接通坐席未接通"）；AI 后续 bridge 不覆盖
        // （非空跳过）。座机代拨(呼出方向坐席腿 answer 走本 process)不能在此补——接通判定只认
        // CHANNEL_BRIDGE(客户接上)，否则坐席摘机后客户未接挂断会被误记为已接通
        if (Objects.equals(DirectionEnum.INBOUND.getType(), callInfo.getDirection())
                && (Objects.isNull(callInfo.getAnswerTime()) || callInfo.getAnswerTime() == 0L)) {
            callInfo.setAnswerTime(lfsChannelInfo.getAnswerTime());
        }
        // 座机代拨（呼出方向坐席腿已摘机，走本 process 的唯一呼出场景）：出局客户腿期间
        // 给坐席腿播回铃音，客户接通 bridge 时由 FsCallBridgeProcess.playBreak 停掉。
        // tone_stream 为 FS 内置铃流(2s on/4s off, 440+480Hz)，不依赖语音文件；
        // 软电话直呼不进此分支（process=CALL_BRIDGE），呼入 AI/IVR 方向不满足条件
        if (Objects.equals(DirectionEnum.OUTBOUND.getType(), callInfo.getDirection())) {
            log.info("座机代拨: 坐席腿摘机,播放回铃音 callId:{} agentLeg:{}", callInfo.getCallId(), lfsChannelInfo.getUniqueId());
            fsClient.playFile(address, lfsChannelInfo.getUniqueId(), RINGBACK_TONE);
        }
        executeRoute(address, callInfo, lfsChannelInfo.getUniqueId());
    }

    /**
     * 查路由 + 日程检查 + 分发 RouteHandler（不含录音/落 callRel；录音由调用方按时机处理）。
     * <p>供两处复用：① CALL_ROUTE process（AI 路由，主叫 answer 后，先录音再 executeRoute）；
     * ② {@code FsChannelParkEslEventHandler.inboundCall}（人工路由延迟 answer，主叫未 answer，
     *   跳过 CALL_ROUTE 直接 executeRoute originate 坐席，坐席接听后 FsCallBridgeProcess answer 主叫 + bridge）。
     */
    public void executeRoute(String address, CallInfo callInfo, String uniqueId) {
        log.info("[早应答] executeRoute 入口 callId:{} callee:{} uniqueId:{}", callInfo.getCallId(), callInfo.getCallee(), uniqueId);
        CallRouteVo callRoute = lfsCallCacheService.getCallRoute(callInfo.getCallee(), callInfo.getCaller(), callInfo.getDirection());
        if (Objects.isNull(callRoute)) {
            log.info("进入callRoute电话但呼入号码查询为空 callId：{}，caller：{}，callee：{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }

        //todo 当日程为空或者无日程安排时直接挂断（具体需求待研究）
        if (Objects.isNull(callRoute.getScheduleId())) {
            log.info("进入callRoute电话但日程安排为空 callId：{}，caller：{}，callee：{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        CallSchedule schedule = callScheduleService.getDetail(callRoute.getScheduleId());
        if (!checkSchedule(schedule)) {
            log.info("进入callIn电话但无合适日程安排 callId：{}，caller：{}，callee：{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        log.info("scheduleName:{} routeType:{} routeValue:{}", schedule.getName(), callRoute.getRouteType(), callRoute.getRouteValue());

        // 记录命中的呼叫路由ID，经 CallInfo 总线透传到挂断收尾 transfer 落库（供话单展示路由名）
        callInfo.setRouteId(callRoute.getId());
        lfsCallCacheService.saveCallInfo(callInfo);
        FsAbstractRouteHandler routeHandler = routeFactory.factory(callRoute.getRouteType());
        log.info("[早应答] executeRoute 分发 callId:{} routeType:{} handler:{} routeValue:{}",
                callInfo.getCallId(), callRoute.getRouteType(),
                routeHandler == null ? "null" : routeHandler.getClass().getSimpleName(), callRoute.getRouteValue());
        if (Objects.nonNull(routeHandler)) {
            routeHandler.handler(address, callInfo, uniqueId, callRoute.getRouteValue());
        }
    }

}
