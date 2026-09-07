// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.

package com.hsc.esl.handler.esl;

import com.hsc.common.annotation.EslEventName;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.EslEventNames;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.AgentStateEnum;
import com.hsc.common.enums.FsHangupCauseEnum;
import com.hsc.common.enums.SipAgentStatusEnum;
import com.hsc.common.event.AiTransferRetryEvent;
import com.hsc.common.event.CallHangupEvent;
import com.hsc.common.event.CallStatusEvent;
import com.hsc.common.event.SkillAgentMissedEvent;
import com.hsc.common.event.TaskDialFinishEvent;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.factory.AbstractFsEslEventHandler;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.utils.EslEventUtil;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.vo.agent.SipAgentStatusVo;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * 渠道挂机处理类
 *
 * @author danmo
 * @date 2023年09月18日 19:03
 */
@Slf4j
@EslEventName(EslEventNames.CHANNEL_HANGUP_COMPLETE)
@Component
public class FsChannelHangUpCompleteEslEventHandler extends AbstractFsEslEventHandler {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IFlowNoticeService iFlowNoticeService;

    @Override
    public void handleEslEvent(String address, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        log.info("ChannelHangUpCompleteEslEventHandler eventName:{} uniqueId:{}", event.getEventName(), uniqueId);
        CallInfo callInfo = ifsCallCacheService.getCallInfoByUniqueId(uniqueId);
        if (Objects.isNull(callInfo)) {
            return;
        }
        log.info("挂机 callId:{} uniqueId:{}", callInfo.getCallId(), uniqueId);
        // 客户中途挂断的 IVR 兜底清理：停状态机+清实例（正常走 end 的已清过，此处按 key 存在幂等）。
        // 覆盖常见场景：IVR 收号中/转接后等坐席时客户挂断——原实现无任何清理，实例与流程缓存残留
        FlowDataContext flowDataContext = callInfo.getFlowDataContext();
        if (Objects.nonNull(flowDataContext) && Objects.nonNull(flowDataContext.getInstanceId())
                && redisService.hasKey(StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, flowDataContext.getInstanceId()))) {
            log.info("客户挂断清理IVR实例 callId:{} instanceId:{}", callInfo.getCallId(), flowDataContext.getInstanceId());
            iFlowNoticeService.notice(3, "", flowDataContext);
        }
        int count = callInfo.getUniqueIdList().size();
        callInfo.removeUniqueIdList(uniqueId);
        ChannelInfo channelInfo = callInfo.getChannelMap().get(uniqueId);
        if (Objects.isNull(channelInfo)) {
            return;
        }
        channelInfo.setHangupCause(EslEventUtil.getHangupCause(event));
        channelInfo.setSipProtocol(EslEventUtil.getSipViaProtocol(event));
        channelInfo.setSipStatus(EslEventUtil.getSipTermStatus(event));
        channelInfo.setChannelName(EslEventUtil.getChannelName(event));
        channelInfo.setEndTime(event.getEventDateTimestamp() / 1000);
        // AI 腿挂断分两种：
        //  ① 转人工中转(aiTransferring=true 且 count>1，主叫腿还活)：不挂其他腿、不走收尾，仅清理
        //  ai 腿(主叫腿要继续等坐席 bridge)；
        //  ② AI 对话结束/异常挂断(非转人工)：必须走下方正常收尾挂断主叫腿——原实现只判 count>1，
        //  导致 MEDIA_TIMEOUT 等 AI 侧异常时主叫腿悬在 PARK 静音直到客户自己挂(实测悬过 5 分钟)
        if (EslConstant.AI_AGENT_NAME.equals(channelInfo.getAgentName()) && count > 1
                && Boolean.TRUE.equals(callInfo.getAiTransferring())) {
            // uniqueIdList 已在方法开头统一移除，此处仅清通道映射
            callInfo.removeChannelInfoMap(uniqueId);
            ifsCallCacheService.saveCallInfo(callInfo);
            log.info("AI 腿挂断(转人工)不收尾 callId:{} aiLegId:{}", callInfo.getCallId(), uniqueId);
            return;
        }

        // AI 转人工 originate 失败重试：坐席腿(被叫腿)没 bridge 就挂了 + aiTransferring 标记
        if (Boolean.TRUE.equals(callInfo.getAiTransferring())
                && Objects.equals(2, channelInfo.getDirectionType())
                && channelInfo.getBridgeTime() == null) {
            String cause = channelInfo.getHangupCause();
            if ("USER_BUSY".equals(cause) || "CALL_REJECTED".equals(cause)
                    || "NO_USER_RESPONSE".equals(cause) || "SUBSCRIBER_ABSENT".equals(cause)) {
                log.info("AI 转人工 originate 失败(忙/不可达) 触发重试 callId:{} cause:{}", callInfo.getCallId(), cause);
                callInfo.removeChannelInfoMap(uniqueId);
                ifsCallCacheService.saveCallInfo(callInfo);
                applicationEventPublisher.publishEvent(new AiTransferRetryEvent(this, callInfo.getCallId(), cause));
                return;
            }
            callInfo.setAiTransferring(false);
            log.warn("AI 转人工 originate 失败(振铃超时) 不重试 callId:{} cause:{}", callInfo.getCallId(), cause);
        }
        // 技能组坐席腿未接通挂断(漏接)：不挂主叫腿，发事件重分配(本通 1 次上限、排除漏接坐席)/
        // 走溢出策略——原实现落 count>1 分支仅存缓存，主叫腿被下方 forEach 强挂、无重分配。
        // AI 转人工链路不设 skillId，不会误入(AiTransferRetryEvent 机制自管)
        if (Objects.equals(2, channelInfo.getDirectionType())
                && channelInfo.getBridgeTime() == null
                && Objects.nonNull(callInfo.getSkillId())) {
            callInfo.setLastFailedAgentId(channelInfo.getAgentId());
            callInfo.removeChannelInfoMap(uniqueId);
            ifsCallCacheService.saveCallInfo(callInfo);
            log.info("坐席腿未接通挂断(漏接) 触发重分配 callId:{} agentId:{}", callInfo.getCallId(), channelInfo.getAgentId());
            applicationEventPublisher.publishEvent(new SkillAgentMissedEvent(this, callInfo.getCallId(), address, channelInfo.getOtherUniqueId()));
            return;
        }
        // 设置挂机方向
        if(Objects.isNull(callInfo.getHangupDir())){
            if(Objects.equals(1,channelInfo.getDirectionType())){
                callInfo.setHangupDir(1);
            }
            if(Objects.equals(2,channelInfo.getDirectionType())){
                callInfo.setHangupDir(2);

            }
            String hangupCause = EslEventUtil.getHangupCause(event);
            FsHangupCauseEnum causeEnum = FsHangupCauseEnum.getByValue(hangupCause);
            if(Objects.nonNull(causeEnum)){
                callInfo.setHangupCause(causeEnum.getCode());
            }
        }
        callInfo.getChannelMap().forEach((key, value) -> fsClient.hangupCall(address, callInfo.getCallId(), key));

        //最后一个挂机
        if (count == 1) {
            //坐席状态变更
            changeAgentStatus(callInfo, channelInfo);
            sendAgentStatus(callInfo.getCallId(), callInfo.getCaller(), callInfo.getCallee(), callInfo.getDirection(), AgentStateEnum.CALL_END);


            CallRecord callRecord = new CallRecord();
            callRecord.transfer(callInfo);
            // 1.11 PARK 已建 CallRecord，此处按 callId update；未建则兜底 save（call_id 有唯一约束）
            CallRecord existing = iCallRecordService.getByCallId(String.valueOf(callInfo.getCallId()));
            if (existing != null) {
                callRecord.setId(existing.getId());
                // agent_name 置 null 跳过更新：分配时(updateCallRecordAgent)已固化"坐席名(登录名)"快照，
                // transfer 带的是内存纯坐席名，覆盖会冲掉快照
                callRecord.setAgentName(null);
                iCallRecordService.updateById(callRecord);
            } else {
                iCallRecordService.save(callRecord);
            }
            // 1.12 最后一路挂断(count==1)发事件触发全文摘要
            applicationEventPublisher.publishEvent(new CallHangupEvent(this, String.valueOf(callInfo.getCallId())));

            // ESL 权威挂断兜底推 CALL_STATUS close 给坐席前端：座机软电话的"通话结束"原先
            // 唯一依赖 ai-callbot 旁路链路(FS 断 fork WS → ai-callbot 检测断开 → Redis close → WS 推送)，
            // 该链任一环卡住(ai-callbot 卡死/重启/断开检测失效)前端就永久计时且无法收尾。
            // 此处与话单收尾/摘要同一权威时点直接发 close；与旁路 close(两腿各一条)重复到达，
            // 由前端按 callId 幂等吸收(只处理当前通话匹配的 close)。软电话不消费 close(挂断走 JsSIP)，
            // 无影响；未分配坐席的通话(IVR 等)定位不到 WS session 会被推送层静默跳过。
            applicationEventPublisher.publishEvent(new CallStatusEvent(this,
                    String.valueOf(callInfo.getCallId()), "close",
                    callInfo.getAgentNumber(), callInfo.getCaller()));

            // 任务/私海拨打回写：呼叫发起时经 SIP 头(软电话)/代拨入参(座机)携带的身份标识由 CallInfo
            // 全程持有，assignmentId（任务拨打）或 customerId（私海拨打）任一非空即发 TaskDialFinishEvent
            // （hsc-esl 不能依赖 hsc-call-task，走事件解耦），由任务模块回写联系人最近结果/拨打历史/客户痕迹。
            // 无标识（手动拨号/呼入/AI 等）跳过，零侵入。事件带 answered+causeCode 原料，
            // 结果判定由消费方调 CallResultEnum.of 唯一入口；callRecordId 供拨打历史关联话单。
            if (Objects.nonNull(callInfo.getAssignmentId()) || Objects.nonNull(callInfo.getCustomerId())) {
                boolean answered = Objects.nonNull(callInfo.getAnswerTime());
                long endStamp = Objects.nonNull(channelInfo.getEndTime()) ? channelInfo.getEndTime() : System.currentTimeMillis();
                int talkDuration = answered ? (int) Math.max(0, (endStamp - callInfo.getAnswerTime()) / 1000) : 0;
                log.info("外呼拨打完成 callId:{} assignmentId:{} taskId:{} customerId:{} agentId:{} answered:{} talkDuration:{}s",
                        callInfo.getCallId(), callInfo.getAssignmentId(), callInfo.getTaskId(), callInfo.getCustomerId(),
                        callInfo.getAgentId(), answered, talkDuration);
                applicationEventPublisher.publishEvent(new TaskDialFinishEvent(this,
                        callInfo.getAssignmentId(), callInfo.getTaskId(), callInfo.getCustomerId(),
                        callInfo.getAgentId(), callInfo.getCallee(), callRecord.getId(), String.valueOf(callInfo.getCallId()),
                        answered, callInfo.getHangupCause(), channelInfo.getHangupCause(),
                        Objects.nonNull(callInfo.getCallTime()) ? new Date(callInfo.getCallTime()) : null,
                        talkDuration, new Date(endStamp)));
            }

            ifsCallCacheService.removeCallInfo(callInfo.getCallId());
            // 最后一腿已收尾并清缓存，直接返回——不能再 saveCallInfo 写回(会把已删的 CallInfo 复活进 Redis 造成缓存泄漏)
            return;
        }
        callInfo.setChannelInfoMap(uniqueId, channelInfo);
        ifsCallCacheService.saveCallInfo(callInfo);

    }


    private void changeAgentStatus(CallInfo callInfo, ChannelInfo channelInfo) {
        if (Objects.isNull(callInfo) || Objects.isNull(callInfo.getAgentId())) {
            return;
        }
        Boolean isAgent = redisService.getCacheMapHasKey(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()));
        if (Boolean.TRUE.equals(isAgent)) {
            SipAgentStatusVo agentStatusVo = redisService.getCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()));
            // 状态时间与话后计时均取本腿真实挂断时间戳——原 callEndTime 塞 gainSkillAfterTime()
            // 兜底秒数 1(死链 CallInfoDetail.afterTime 全库无赋值)，策略4「最长话后」因此失效
            agentStatusVo.setStatusTime(channelInfo.getEndTime());
            agentStatusVo.setCallEndTime(channelInfo.getEndTime());
            agentStatusVo.setStatus(SipAgentStatusEnum.NOT_READY.getCode());
            agentStatusVo.setOnlineStatus(SipAgentStatusEnum.NOT_READY.getCode());
            // 写回键与读取键统一用 agentId（原写 vo.getId()，两者不等时更新错条目）
            redisService.setCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()), agentStatusVo);
        }
    }
}
