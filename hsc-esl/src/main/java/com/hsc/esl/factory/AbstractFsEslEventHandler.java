// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.factory;

import com.hsc.common.config.redis.RedisService;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.AgentStateEnum;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.system.service.ICallDisplayService;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.IPhoneLocationService;
import com.hsc.system.service.ISipAgentService;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author danmo
 * @date 2023年09月28日 14:21
 */
@Slf4j
@Component
public abstract class AbstractFsEslEventHandler implements FsEslEventHandler {

    @Autowired
    protected IFsCallCacheService ifsCallCacheService;

    @Autowired
    protected FsEslProcessFactory fsEslProcessFactory;

    @Autowired
    protected ISipAgentService iSipAgentService;

    @Autowired
    protected ICallDisplayService iCallDisplayService;

    @Autowired
    protected RedisService redisService;

    @Lazy
    @Autowired
    protected FsClient fsClient;

    @Autowired
    protected ICallRecordService iCallRecordService;
    @Autowired
    protected IPhoneLocationService iPhoneLocationService;


    @Override
    public abstract void handleEslEvent(String address, EslEvent event);

    /**
     * 被叫腿(出局腿)振铃时给主叫腿(坐席软电话腿)补发 ring_ready。
     * <p>晚应答架构下两腿 bridge 前无信令关联，B 腿收到的网关 180/183 到不了坐席腿，
     * 软电话打出后全程静默；由 PROGRESS / PROGRESS_MEDIA 两个 handler 调用，
     * FS 收到后向坐席腿发 180 Ringing → 软电话 progress 事件 → 播本地回铃音。
     * <p>仅坐席外呼(OUTBOUND)使用；呼入转外呼的主叫腿是客户(回铃由客户侧运营商提供)，不补发。
     * 仅处理被叫腿(directionType=2)的事件——坐席腿收到补发的 180 会再触发一次
     * CHANNEL_PROGRESS，若不按腿角色过滤将形成 A腿→B腿→A腿 的回环。
     *
     * @param address         FS 地址
     * @param callInfo        呼叫总线
     * @param progressLegUuid 触发 progress 事件的腿 uuid(应为出局被叫腿)
     */
    protected void notifyCallerRingReady(String address, CallInfo callInfo, String progressLegUuid) {
        if (!Objects.equals(DirectionEnum.OUTBOUND.getType(), callInfo.getDirection())) {
            return;
        }
        ChannelInfo legChannel = callInfo.getChannelMap().get(progressLegUuid);
        if (Objects.isNull(legChannel) || !Objects.equals(2, legChannel.getDirectionType())) {
            return;
        }
        if (StringUtils.isEmpty(legChannel.getOtherUniqueId())) {
            return;
        }
        fsClient.ringReady(address, legChannel.getOtherUniqueId());
    }


    protected void sendAgentStatus(Long callId, String caller, String callee, Integer direction, AgentStateEnum status) {
        /*LfsWsMsg wsMsg = new LfsWsMsg();
        wsMsg.setCallId(callId);
        wsMsg.setCallee(caller);
        wsMsg.setCaller(callee);
        wsMsg.setDirection(direction);
        wsMsg.setStatus(status.getCode());
        wsMsg.setContent(status.getDes());
        LfsMqMsg mqMsg = LfsMqMsg.builder().msg(JSONObject.toJSONString(wsMsg)).userId(1L).build();
        LfsBaseMqMsg baseMqMsg = LfsBaseMqMsg.builder().msg(mqMsg).topic("callEventStatus-out").build();
        Boolean send = lfsRocketMqMsgProducer.send(baseMqMsg);
        if (send) {
            log.info("[发送MQ消息-坐席websocket消息通知]成功 wsMsg:{}", wsMsg);
        } else {
            log.info("[发送MQ消息-坐席websocket消息通知]失败 wsMsg:{}", wsMsg);
        }*/
    }
}
