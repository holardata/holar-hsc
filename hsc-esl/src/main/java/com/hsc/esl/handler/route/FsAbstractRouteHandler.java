// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import com.hsc.common.config.redis.RedisService;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author danmo
 * @date 2023-11-10 17:19
 **/
@Slf4j
@Component
public abstract class FsAbstractRouteHandler {

    @Lazy
    @Autowired
    protected FsClient fsClient;


    @Autowired
    protected RedisService redisService;

    @Lazy
    @Autowired
    protected IFsCallCacheService fsCallCacheService;

    @Resource
    protected ISipAgentService iSipAgentService;

    @Autowired
    protected ICallSkillService iCallSkillService;

    @Autowired
    protected IFsSipGatewayService iFsSipGatewayService;

    @Autowired
    protected IVoiceFileService iVoiceFileService;

    @Autowired
    protected ICallRecordService iCallRecordService;

    public abstract void handler(String address, CallInfo callInfo, String uniqueId,  String routeValue);

    protected void saveCallInfo(CallInfo callInfo){
        fsCallCacheService.saveCallInfo(callInfo);
    }

    /**
     * 1.11 坐席分配后同步更新 CallRecord 的 agent 字段，保证通话进行中 ws 推送能按 callId→agentNumber 定位坐席。
     * 呼入场景 PARK 建记录时坐席未分配(agent 兜底)，经路由分配坐席后在此补全。
     * agent_name 落"坐席名(登录名)"快照（固化当时绑定关系，防后续改绑对不上人）；
     * AI 坐席(agentId=null)落原 agentName("AI智能坐席")。CallInfo 内存对象仍存纯坐席名
     * （AI 腿判定按 "AI智能坐席" 精确匹配，不能被快照格式污染）。
     */
    protected void updateCallRecordAgent(CallInfo callInfo) {
        CallRecord existing = iCallRecordService.getByCallId(String.valueOf(callInfo.getCallId()));
        if (existing == null) {
            return;
        }
        CallRecord record = new CallRecord();
        record.setId(existing.getId());
        record.setAgentId(callInfo.getAgentId());
        record.setAgentNumber(callInfo.getAgentNumber());
        record.setAgentName(iSipAgentService.buildNameSnapshot(callInfo.getAgentId(), callInfo.getAgentName()));
        iCallRecordService.updateById(record);
    }

    /**
     * 主叫腿与对端腿的 otherUniqueId 互指。建对端腿（originate 坐席/ai 腿）后调用：
     * 对端腿.otherUniqueId 已在 builder 设（指向主叫腿），这里补设主叫腿.otherUniqueId=对端腿。
     * 避免主叫腿 answer 触发的 CALL_BRIDGE 因主叫腿.otherUniqueId=null 取不到对端腿（日志 WARN 腿缺失）。
     */
    protected void bindMutualOtherUniqueId(CallInfo callInfo, String callerLegId, String otherLegId) {
        if (callerLegId == null || otherLegId == null) {
            return;
        }
        ChannelInfo callerChannel = callInfo.getChannelMap().get(callerLegId);
        if (callerChannel != null) {
            callerChannel.setOtherUniqueId(otherLegId);
            callInfo.setChannelInfoMap(callerLegId, callerChannel);
        }
    }


    /**
     * 路由值转 Long（技能组/网关/坐席等 id 型 route_value 公用）。
     * 非数字(配置错误)记录日志并挂断当前腿，返回 null，调用方判 null 直接 return。
     */
    protected Long parseLongRouteValue(String routeValue, String address, CallInfo callInfo, String uniqueId) {
        try {
            return Long.valueOf(routeValue);
        } catch (NumberFormatException e) {
            log.error("路由值非数字(配置错误) callId:{} routeValue:{}", callInfo.getCallId(), routeValue);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return null;
        }
    }
}
