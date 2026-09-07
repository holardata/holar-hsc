package com.hsc.api.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.SipAgentStatusEnum;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.entity.AiCallbotConfig;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.fssip.FsSipGatewayQuery;
import com.hsc.system.domain.vo.agent.SipAgentStatusVo;
import com.hsc.system.domain.vo.sip.FsRegVo;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.skill.CallSkillVo;
import com.hsc.system.service.IAiCallbotConfigService;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.ICallSkillService;
import com.hsc.system.service.IDialogRecordService;
import com.hsc.system.service.IFsSipGatewayService;
import com.hsc.system.service.ISipAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 转人工编排：ai-callbot 判断要转人工时调 hsc {@code POST /system/v1/ai/transfer}，由本 service 统一处理
 * —— 落"转人工"锚点(dialog_record.msg_type=transfer) → 按技能组选空闲坐席并校验前置(号码/internal 网关)
 * → 全满足才 uuid_kill ai 腿释放媒体 → originate 坐席腿(续原 callId) → CALL_BRIDGE 自动 bridge。
 *
 * <p>主叫腿 callId 全程不变(只是 bridge 对端从 ai-callbot 换成坐席)，话单天然一条。
 *
 * <p>转人工目标 = ai_callbot_config.config_json.transfer_skill_id 配的技能组，从中选一个 READY 空闲坐席
 * （前端 AI 配置页配技能组，替代旧的固定 transfer-target 环境变量；不排队，无空闲则失败）。
 *
 * <p>失败语义：任一前置条件(选不到坐席/无号码/无 internal 网关)不满足时，<b>不 kill ai 腿</b>、返回 false，
 * AiTransferController 据此返回失败码，ai-callbot 识别后播 transfer_fallback_prompt 并礼貌挂断（而非主叫静音死寂）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTransferService {

    private final FsClient fsClient;
    private final IFsCallCacheService fsCallCacheService;
    private final IDialogRecordService dialogRecordService;
    private final IFsSipGatewayService fsSipGatewayService;
    private final ISipAgentService sipAgentService;
    private final ICallRecordService callRecordService;
    private final ICallSkillService callSkillService;
    private final RedisService redisService;
    private final IAiCallbotConfigService aiCallbotConfigService;
    private final ISipRegService sipRegService;

    /**
     * @param callIdStr hsc 透传给 ai-callbot 的雪花 callId（字符串）
     * @param trigger   触发方式（keyword / ai_intent / dtmf），仅用于锚点备注
     */
    public boolean transferToHuman(String callIdStr, String trigger) {
        Long callId;
        try {
            callId = Long.valueOf(callIdStr);
        } catch (Exception e) {
            log.error("AI 转人工失败：callId 非法 callIdStr:{}", callIdStr);
            return false;
        }
        log.info("AI 转人工 callId:{} trigger:{}", callId, trigger);

        CallInfo callInfo = fsCallCacheService.getCallInfo(callId);
        if (callInfo == null) {
            log.error("AI 转人工失败：CallInfo 不存在 callId:{}", callId);
            return false;
        }
        String address = fsClient.getRandomAddress();
        if (StringUtils.isBlank(address)) {
            log.error("AI 转人工失败：无可用 FS 连接 callId:{}", callId);
            return false;
        }

        // 1. 定位主叫腿(directionType=1) 与 ai 腿(directionType=2)
        String callerLegId = null;
        String aiLegId = null;
        Map<String, ChannelInfo> channelMap = callInfo.getChannelMap();
        if (channelMap != null) {
            for (Map.Entry<String, ChannelInfo> e : channelMap.entrySet()) {
                ChannelInfo ch = e.getValue();
                if (ch == null || ch.getDirectionType() == null) {
                    continue;
                }
                if (ch.getDirectionType() == 1) {
                    callerLegId = e.getKey();
                } else if (ch.getDirectionType() == 2) {
                    aiLegId = e.getKey();
                }
            }
        }
        if (StringUtils.isBlank(callerLegId)) {
            log.error("AI 转人工失败：未找到主叫腿 callId:{}", callId);
            return false;
        }

        // 3. 选候选目标列表 + 查网关
        java.util.List<TransferTarget> targets = pickFreeTargets();
        if (CollectionUtil.isEmpty(targets)) {
            log.error("AI 转人工失败：无可用目标（技能组无 READY 坐席 + 无注册座机）callId:{}", callId);
            return false;
        }
        FsSipGatewayQuery query = new FsSipGatewayQuery();
        query.setGatewayType(0);
        List<FsSipGateway> gatewayList = fsSipGatewayService.getList(query);
        if (CollectionUtil.isEmpty(gatewayList)) {
            log.error("AI 转人工失败：未找到 internal 网关 callId:{}", callId);
            return false;
        }

        // 4. 前置条件全满足：舒适噪音(兜底) + 回铃音(嘟嘟) + kill ai 腿释放媒体
        // ⚠️ 转人工标记必须先于 uuidKill 保存到 Redis：AI 腿挂断事件与 ESL 线程竞态，
        //    若挂断 handler 读到的 callInfo 无 aiTransferring 标记，"AI 腿不收尾"保护失效，
        //    会误判整通结束 forEach 挂掉活着的主叫腿(客户被挂、转人工必败)
        callInfo.setAiTransferring(true);
        fsCallCacheService.saveCallInfo(callInfo);
        fsClient.uuidSetvar(address, callerLegId, "send_silence_when_idle", "1");
        fsClient.sendArgs(address, callerLegId, "endless_playback", "tone_stream://%(1000,4000,450)");
        if (StringUtils.isNotBlank(aiLegId)) {
            fsClient.uuidKill(address, aiLegId, "NORMAL_CLEARING");
            callInfo.removeChannelInfoMap(aiLegId);
            callInfo.removeUniqueIdList(aiLegId);
            log.info("AI 腿已挂断 callId:{} aiLegId:{}", callId, aiLegId);
        }

        // 5. originate 第一个目标 + 记候选列表/索引/转人工标记（重试用）
        originateTarget(address, callInfo, callerLegId, targets.get(0), gatewayList.get(0));
        callInfo.setTransferTargets(JSON.toJSONString(targets));
        callInfo.setTransferIndex(0);
        fsCallCacheService.saveCallInfo(callInfo);

        saveTransferMark(String.valueOf(callId), trigger);
        markTransferTime(callId);
        return true;
    }

    /** 转人工目标（坐席或座机） */
    private record TransferTarget(String number, String name, boolean landline, Long agentId) {}

    /**
     * 从技能组选所有可用目标（有序列表，先 READY 坐席后注册座机），用于首次 originate + 重试。
     */
    private java.util.List<TransferTarget> pickFreeTargets() {
        AiCallbotConfig cfg = aiCallbotConfigService.getSingle();
        if (cfg == null || StringUtils.isBlank(cfg.getConfigJson())) {
            return java.util.List.of();
        }
        JSONObject json = JSON.parseObject(cfg.getConfigJson());
        String skillIdStr = json == null ? null : json.getString("transfer_skill_id");
        if (StringUtils.isBlank(skillIdStr)) {
            log.warn("AI 转人工：未配 transfer_skill_id，无法选目标");
            return java.util.List.of();
        }
        Long skillId;
        try {
            skillId = Long.valueOf(skillIdStr);
        } catch (Exception e) {
            log.warn("AI 转人工：transfer_skill_id 非法 {}", skillIdStr);
            return java.util.List.of();
        }
        CallSkillVo callSkill = callSkillService.getDetail(skillId);
        if (callSkill == null || CollectionUtil.isEmpty(callSkill.getAgentList())) {
            log.warn("AI 转人工：技能组无成员 transfer_skill_id:{}", skillIdStr);
            return java.util.List.of();
        }
        var members = callSkill.getAgentList();
        java.util.List<TransferTarget> targets = new java.util.ArrayList<>();

        // 1. 坐席(memberType=0/null)：查 Redis READY，所有空闲的都加入候选
        List<String> agentIds = members.stream()
                .filter(m -> m.getMemberType() == null || m.getMemberType() == 0)
                .map(m -> String.valueOf(m.getAgentId()))
                .toList();
        if (!agentIds.isEmpty()) {
            List<SipAgentStatusVo> statusList = redisService.getMultiCacheMapValue(
                    CacheConstants.AGENT_CURRENT_STATUS_KEY, agentIds);
            for (SipAgentStatusVo s : statusList) {
                if (s != null && Objects.equals(SipAgentStatusEnum.READY.getCode(), s.getStatus())) {
                    SipAgentVo agent = sipAgentService.getDetail(s.getId());
                    if (agent != null && StringUtils.isNotBlank(agent.getAgentNumber())) {
                        targets.add(new TransferTarget(agent.getAgentNumber(), agent.getName(), false, agent.getId()));
                    }
                }
            }
        }

        // 2. 座机(memberType=1)：查 FS 注册，所有在线的都加入候选
        List<String> extensionNumbers = members.stream()
                .filter(m -> m.getMemberType() != null && m.getMemberType() == 1)
                .map(m -> String.valueOf(m.getAgentId()))
                .toList();
        if (!extensionNumbers.isEmpty()) {
            List<FsRegVo> regs = sipRegService.getList(null);
            for (String ext : extensionNumbers) {
                if (regs.stream().anyMatch(r -> ext.equals(r.getUsername()))) {
                    targets.add(new TransferTarget(ext, "座机:" + ext, true, null));
                }
            }
        }

        log.info("AI 转人工：选出 {} 个可用目标 transfer_skill_id:{}", targets.size(), skillIdStr);
        return targets;
    }

    /**
     * originate 一个转人工目标（坐席腿）。transferToHuman 首次 + retryNextTarget 重试都用它。
     * @return 坐席腿 uniqueId
     */
    private String originateTarget(String address, CallInfo callInfo, String callerLegId,
                                   TransferTarget target, FsSipGateway gateway) {
        callInfo.setCallee(target.number());
        callInfo.setAgentId(target.agentId());
        callInfo.setAgentNumber(target.number());
        callInfo.setAgentName(target.name());
        updateCallRecordAgent(callInfo);

        String otherUniqueId = RandomUtil.randomNumbers(32);
        ChannelInfo otherChannelInfo = ChannelInfo.builder()
                .callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(2).type(2).directionType(2)
                .agentId(target.agentId()).agentNumber(target.number()).agentName(target.name())
                .callTime(DateUtil.current()).otherUniqueId(callerLegId)
                .called(target.number()).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.setChannelInfoMap(otherUniqueId, otherChannelInfo);
        ChannelInfo callerChannel = callInfo.getChannelMap().get(callerLegId);
        if (callerChannel != null) {
            callerChannel.setOtherUniqueId(otherUniqueId);
            callInfo.setChannelInfoMap(callerLegId, callerChannel);
        }
        callInfo.addUniqueIdList(otherUniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);

        // 先落 callRel 再 originate：坐席腿事件若先于映射写入到达会被各 handler 静默丢弃
        //（与 FsExtensionRouteHandler 同款竞态修法），重试机制也随之失效
        fsCallCacheService.saveCallRel(otherUniqueId, callInfo.getCallId());
        fsClient.makeCall(address, callInfo.getCallId(), target.number(), callInfo.getCallerDisplay(),
                otherUniqueId, callInfo.getCalleeTimeOut(), gateway);
        log.info("AI 转人工 originate 目标 callId:{} number:{} otherUniqueId:{}", callInfo.getCallId(), target.number(), otherUniqueId);
        return otherUniqueId;
    }

    /**
     * AI 转人工 originate 失败重试：HangupComplete 发 AiTransferRetryEvent 触发。
     * 选候选列表的下一个目标 originate；用完了挂主叫（转人工失败）。
     */
    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void onTransferRetry(com.hsc.common.event.AiTransferRetryEvent event) {
        Long callId = event.getCallId();
        CallInfo callInfo = fsCallCacheService.getCallInfo(callId);
        if (callInfo == null || !Boolean.TRUE.equals(callInfo.getAiTransferring())) {
            return;
        }
        int nextIndex = (callInfo.getTransferIndex() != null ? callInfo.getTransferIndex() : 0) + 1;
        JSONArray targets = JSON.parseArray(callInfo.getTransferTargets());
        if (targets == null || nextIndex >= targets.size()) {
            // 候选用完 → 转人工失败，挂主叫
            log.warn("AI 转人工重试：候选目标已用完，转人工失败 callId:{}", callId);
            callInfo.setAiTransferring(false);
            fsCallCacheService.saveCallInfo(callInfo);
            String address = fsClient.getRandomAddress();
            for (Map.Entry<String, ChannelInfo> e : callInfo.getChannelMap().entrySet()) {
                if (e.getValue() != null && Objects.equals(1, e.getValue().getDirectionType())) {
                    fsClient.hangupCall(address, callId, e.getKey());
                    break;
                }
            }
            return;
        }
        // originate 下一个
        JSONObject obj = targets.getJSONObject(nextIndex);
        TransferTarget next = new TransferTarget(obj.getString("number"), obj.getString("name"),
                obj.getBooleanValue("landline"), obj.getLong("agentId"));
        String address = fsClient.getRandomAddress();
        String callerLegId = null;
        for (Map.Entry<String, ChannelInfo> e : callInfo.getChannelMap().entrySet()) {
            if (e.getValue() != null && Objects.equals(1, e.getValue().getDirectionType())) {
                callerLegId = e.getKey();
                break;
            }
        }
        FsSipGatewayQuery query = new FsSipGatewayQuery();
        query.setGatewayType(0);
        List<FsSipGateway> gatewayList = fsSipGatewayService.getList(query);
        if (StringUtils.isBlank(callerLegId) || CollectionUtil.isEmpty(gatewayList)) {
            log.warn("AI 转人工重试：主叫腿或网关缺失 callId:{}", callId);
            return;
        }
        callInfo.setTransferIndex(nextIndex);
        originateTarget(address, callInfo, callerLegId, next, gatewayList.get(0));
        fsCallCacheService.saveCallInfo(callInfo);
        log.info("AI 转人工重试 originate 下一个 callId:{} index:{} number:{}", callId, nextIndex, next.number());
    }

    /** 转人工触发方式（ai-callbot 回传 trigger）→ 展示文案；未知值不拼注（避免英文枚举直出） */
    private static final Map<String, String> TRANSFER_TRIGGER_LABELS = Map.of(
            "keyword", "关键词触发",
            "dtmf", "按键触发",
            "ai_intent", "AI识别意图"
    );

    private void saveTransferMark(String callIdStr, String trigger) {
        DialogRecord dialog = new DialogRecord();
        dialog.setCallId(callIdStr);
        dialog.setChannelType("系统");
        dialog.setMsgType("transfer");
        String triggerLabel = TRANSFER_TRIGGER_LABELS.get(trigger);
        dialog.setDialogTxt("已转接人工坐席" + (triggerLabel != null ? "（" + triggerLabel + "）" : ""));
        dialog.setSource("transfer");
        dialog.setSpeakTime(new Date());
        dialog.setStatus(0);
        dialogRecordService.save(dialog);
    }

    /** 标记转人工时间（transfer_time），话单详情可显示"AI 对话 X 秒后转人工" */
    private void markTransferTime(Long callId) {
        CallRecord existing = callRecordService.getByCallId(String.valueOf(callId));
        if (existing == null) {
            return;
        }
        CallRecord up = new CallRecord();
        up.setId(existing.getId());
        up.setTransferTime(new Date());
        up.setTransferHuman(1);
        callRecordService.updateById(up);
    }

    private void updateCallRecordAgent(CallInfo callInfo) {
        CallRecord existing = callRecordService.getByCallId(String.valueOf(callInfo.getCallId()));
        if (existing == null) {
            return;
        }
        CallRecord up = new CallRecord();
        up.setId(existing.getId());
        up.setAgentId(callInfo.getAgentId());
        up.setAgentNumber(callInfo.getAgentNumber());
        // agent_name 落"坐席名(登录名)"快照（同 FsAbstractRouteHandler.updateCallRecordAgent，
        // 固化当时坐席绑定的系统用户，防后续改绑对不上人）
        up.setAgentName(sipAgentService.buildNameSnapshot(callInfo.getAgentId(), callInfo.getAgentName()));
        callRecordService.updateById(up);
    }
}
