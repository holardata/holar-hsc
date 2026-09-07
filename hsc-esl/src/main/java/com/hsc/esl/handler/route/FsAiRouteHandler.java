package com.hsc.esl.handler.route;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.system.domain.entity.CallRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 智能坐席路由：把主叫腿 bridge 到 ai-callbot（无注册 IP 直连 SIP UAS）。
 * <p>originate 走 {@link com.hsc.esl.client.FsClient#makeCallToAiBot}，endpoint 为
 * {@code sofia/internal/sip:<ip>:<port>}（ai-callbot 不注册，不能用 user/@realm），并透传
 * {@code sip_h_X-HSC-CallId} 让 ai-callbot 用 hsc 的 callId 作 redis_call_id（话单合并生死线）。
 * <p>ai 腿挂断/转人工的收尾由后续阶段（uuid_kill + 转人工 API）处理；本 handler 只负责把 ai 腿
 * originate 出去并续原 callId（复用 {@code FsAgentRouteHandler} 的 ChannelInfo + saveCallRel 模式），
 * 主叫腿与 ai 腿的 bridge 由 {@code FsCallBridgeProcess} 在 CHANNEL_BRIDGE 事件时自动完成。
 *
 * @author hsc
 */
@EslRouteName(RouteTypeEnum.AI)
@Component
@Slf4j
public class FsAiRouteHandler extends FsAbstractRouteHandler {

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String routeValue) {
        // routeValue 为号码路由里"智能坐席"目标值（候选标识），ai-callbot 实际 SIP 地址从配置
        // hsc.ai-callbot.sip-uri 读（单一 ai-callbot 实例）；多实例后续再扩展。
        String otherUniqueId = RandomUtil.randomNumbers(32);
        log.info("转 AI 智能坐席 callId:{} routeValue:{} aiLegUniqueId:{}", callInfo.getCallId(), routeValue, otherUniqueId);

        // AI 段无人工坐席，标记 agentName=AI 便于通话中/列表区分（agentId/Number 留空）
        callInfo.setAgentName(EslConstant.AI_AGENT_NAME);
        updateCallRecordAgent(callInfo);
        // 标记本通"AI先接听"（话单列表可筛/统计 AI 解决率）
        markAiFirst(callInfo.getCallId());

        // 构建 ai 腿通道（directionType=2 被叫腿，otherUniqueId 指回主叫腿）
        ChannelInfo otherChannelInfo = ChannelInfo.builder()
                .callId(callInfo.getCallId())
                .uniqueId(otherUniqueId)
                .cdrType(1)
                .type(1)
                .directionType(2)
                .agentName(EslConstant.AI_AGENT_NAME)
                .callTime(DateUtil.current())
                .otherUniqueId(uniqueId)
                .called("ai-callbot")
                .caller(callInfo.getCaller())
                .display(callInfo.getCallerDisplay())
                .build();
        callInfo.setChannelInfoMap(otherUniqueId, otherChannelInfo);
        bindMutualOtherUniqueId(callInfo, uniqueId, otherUniqueId);
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);

        // 先落缓存再 originate：ai 腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        // originate 到 ai-callbot（带 sip_h_X-HSC-CallId 透传 callId）
        fsClient.makeCallToAiBot(address, callInfo.getCallId(), otherUniqueId,
                callInfo.getCallerDisplay(), callInfo.getCalleeTimeOut());
    }

    /** 标记本通"AI先接听"（is_ai_first=1） */
    private void markAiFirst(Long callId) {
        CallRecord existing = iCallRecordService.getByCallId(String.valueOf(callId));
        if (existing == null) {
            return;
        }
        CallRecord up = new CallRecord();
        up.setId(existing.getId());
        up.setIsAiFirst(1);
        iCallRecordService.updateById(up);
    }
}
