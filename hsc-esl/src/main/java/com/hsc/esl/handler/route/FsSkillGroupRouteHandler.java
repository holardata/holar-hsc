// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.route;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.common.annotation.EslRouteName;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.HangupCauseEnum;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import com.hsc.common.enums.SipAgentStatusEnum;
import com.hsc.common.event.SkillAgentMissedEvent;
import com.hsc.common.thread.ThreadFactoryImpl;
import com.hsc.common.utils.SpringUtils;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.queue.CallQueue;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.fssip.FsSipGatewayQuery;
import com.hsc.system.domain.query.skill.CallSkillQuery;
import com.hsc.system.domain.vo.agent.SipAgentStatusVo;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.route.CallRouteVo;
import com.hsc.system.domain.vo.skill.CallSkillAgentRelVo;
import com.hsc.system.domain.vo.skill.CallSkillVo;
import com.hsc.system.domain.vo.sip.FsRegVo;
import com.hsc.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author danmo
 * @date 2023-11-10 17:20
 **/
@RequiredArgsConstructor
@EslRouteName(RouteTypeEnum.SKILL_GROUP)
@Component
@Slf4j
public class FsSkillGroupRouteHandler extends FsAbstractRouteHandler implements InitializingBean, DisposableBean {

    private final ISipRegService sipRegService;

    private final ISysConfigService iSysConfigService;

    // 排队队列(按 startTime FIFO)：事件线程 add 与 fsAcd 定时线程迭代/移除并发访问，
    // PriorityQueue 非线程安全(并发触发 CME/堆损坏)，用 ConcurrentLinkedQueue
    private final Map<Long, ConcurrentLinkedQueue<CallQueue>> callQueueMap = new ConcurrentHashMap<>();

    private ThreadPoolExecutor callAgentExecutor = new ThreadPoolExecutor(5, 10, 60L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryImpl("fs-agent-distribution-pool-%d"));
    /**
     * 定时线程组
     */
    private static ScheduledExecutorService fsAcdThread = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl("fs-acd-pool-%d"));

    @Override
    public void handler(String address, CallInfo callInfo, String uniqueId, String skillId) {
        log.info("转技能组 callId:{} transfer to {}", callInfo.getCallId(), skillId);
        Long skillRouteId = parseLongRouteValue(skillId, address, callInfo, uniqueId);
        if (skillRouteId == null) {
            return;
        }
        CallSkillVo callSkill = iCallSkillService.getDetail(skillRouteId);
        if (Objects.isNull(callSkill)) {
            log.info("转技能组获取技能组失败 callee:{}, skillId:{}", callInfo.getCallee(), skillId);
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        callInfo.setSkillId(callSkill.getId());

        //电话经过技能组
        CallInfoDetail detail = new CallInfoDetail();
        detail.setCallId(callInfo.getCallId());
        detail.setCreateTime(DateUtil.current());
        detail.setStartTime(DateUtil.current());
        detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
        detail.setTransferType(3);
        detail.setTransferId(callInfo.getSkillId());
        callInfo.addDetailList(detail);

        List<CallSkillAgentRelVo> agentList = callSkill.getAgentList();
        if (CollectionUtil.isEmpty(agentList)) {
            log.info("转技能组未配置坐席失败 callee:{}, skillId:{}", callInfo.getCallee(), skillId);
            callInfo.setSkillHangUpReason(HangupCauseEnum.FULLBUSY.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        // level 取 null 时 Collectors.toMap 直接 NPE(fsAcd 里会炸掉整轮扫描)，兜底 0
        Map<Long, Integer> skillLevelMap = agentList.stream()
                .collect(Collectors.toMap(CallSkillAgentRelVo::getAgentId,
                        m -> m.getLevel() == null ? 0 : m.getLevel(), (key1, key2) -> key1));
        List<SipAgentStatusVo> freeAgentList = getFreeMembers(agentList, skillLevelMap, callInfo);

        //无空闲坐席
        if (CollectionUtil.isEmpty(freeAgentList)) {
            // full_busy_type 可空(DDL DEFAULT 0)，null 兜底 0=排队，防 switch 拆箱 NPE
            int fullBusyType = callSkill.getFullBusyType() == null ? 0 : callSkill.getFullBusyType();
            switch (fullBusyType) {
                //排队
                case 0:
                    callQueueStrategy(address, callInfo, uniqueId, callSkill);
                    break;
                //溢出
                case 1:
                    overFlowStrategy(address, callInfo, uniqueId, callSkill);
                    break;
                //挂机
                case 2:
                    callInfo.setHangupDir(3);
                    callInfo.setHangupCause(HangupCauseEnum.OVERFLOW.getCode());
                    fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
                    saveCallInfo(callInfo);
                    break;
                default:
                    break;
            }
            return;
        }

        //获取坐席策略
        SipAgentVo agentInfo = getAgentStrategy(callInfo, callSkill, freeAgentList);
        if (Objects.isNull(agentInfo)) {
            log.info("转技能组 获取空闲坐席失败 callee:{}, skillId:{}", callInfo.getCallee(), skillId);
            callInfo.setSkillHangUpReason(HangupCauseEnum.FULLBUSY.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        transferAgentHandler(address,agentInfo, callInfo, uniqueId, callSkill);

    }

    /**
     * 获取技能组所有空闲可用成员（handler 首次分发与 fsAcd 排队扫描共用）。
     *
     * <p>坐席成员(memberType=0/null)：查 Redis READY 状态；
     * 座机成员(memberType=1)：agent_id 存分机号字符串，查 FS 注册表(与 AiTransferService
     * 选目标逻辑一致)——注册在线即视为可用，无就绪状态概念。座机合成 SipAgentStatusVo
     * 候选(id=null, agentNumber=分机号)，参与统一策略选择。
     *
     * @param agentList     技能组成员列表
     * @param skillLevelMap 成员级别(坐席id→level)
     * @param callInfo      呼叫对象(漏接排除：本通内不再选 lastFailedAgentId 坐席)
     */
    private List<SipAgentStatusVo> getFreeMembers(List<CallSkillAgentRelVo> agentList, Map<Long, Integer> skillLevelMap, CallInfo callInfo) {
        List<SipAgentStatusVo> freeAgentList = new LinkedList<>();

        // 1. 坐席成员：查 Redis READY
        List<String> agentIds = agentList.stream()
                .filter(m -> m.getMemberType() == null || m.getMemberType() == 0)
                .map(CallSkillAgentRelVo::getAgentId)
                .map(String::valueOf)
                .toList();
        if (!agentIds.isEmpty()) {
            List<SipAgentStatusVo> statusList = redisService.getMultiCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, agentIds);
            if (CollectionUtil.isNotEmpty(statusList)) {
                statusList.stream()
                        .filter(item -> item != null && Objects.equals(SipAgentStatusEnum.READY.getCode(), item.getStatus()))
                        // 漏接排除(座机成员 id 为 null 无法按 id 排除，靠重试上限兜底)
                        .filter(item -> !Objects.equals(callInfo.getLastFailedAgentId(), item.getId()))
                        .forEach(item -> {
                            item.setLevel(skillLevelMap.getOrDefault(item.getId(), 0));
                            freeAgentList.add(item);
                        });
            }
        }

        // 2. 座机成员(memberType=1)：查 FS 注册表，在线即加入候选
        List<CallSkillAgentRelVo> deskMembers = agentList.stream()
                .filter(m -> m.getMemberType() != null && m.getMemberType() == 1)
                .toList();
        if (!deskMembers.isEmpty()) {
            List<FsRegVo> regs = sipRegService.getList(null);
            for (CallSkillAgentRelVo member : deskMembers) {
                String ext = String.valueOf(member.getAgentId());
                if (regs.stream().anyMatch(r -> ext.equals(r.getUsername()))) {
                    SipAgentStatusVo deskCandidate = new SipAgentStatusVo();
                    deskCandidate.setId(null);
                    deskCandidate.setName("座机:" + ext);
                    deskCandidate.setAgentNumber(ext);
                    deskCandidate.setStatus(SipAgentStatusEnum.READY.getCode());
                    // 轮询策略按 level 排序，座机取技能组配置的级别（缺省 0），防空指针
                    deskCandidate.setLevel(member.getLevel() == null ? 0 : member.getLevel());
                    long now = DateUtil.current();
                    deskCandidate.setStatusTime(now);
                    deskCandidate.setCallEndTime(now);
                    deskCandidate.setReceptionNum(0);
                    freeAgentList.add(deskCandidate);
                }
            }
        }
        return freeAgentList;
    }

    private void callQueueStrategy(String address, CallInfo callInfo, String uniqueId, CallSkillVo skill) {
        // computeIfAbsent 原子建队：并发首呼时 get→new→put 会互相覆盖丢呼叫
        ConcurrentLinkedQueue<CallQueue> callQueues = callQueueMap.computeIfAbsent(callInfo.getSkillId(), k -> new ConcurrentLinkedQueue<>());
        // queue_length 可空(DDL DEFAULT NULL、前端必填，null 属异常数据)，兜底容量读系统参数(参数页可调)
        int queueLength = skill.getQueueLength() == null
                ? iSysConfigService.getInt(SysConfigKeys.QUEUE_CAPACITY_DEFAULT, EslConstant.CALL_SKILL_DEFAULT_QUEUE_LENGTH)
                : skill.getQueueLength();
        if(callQueues.size() >= queueLength){
            overFlowStrategy(address, callInfo, uniqueId, skill);
            return;
        }
        callInfo.setQueueStartTime(DateUtil.current());
        if (callInfo.getFirstQueueTime() == null) {
            callInfo.setFirstQueueTime(callInfo.getQueueStartTime());
        }
        // 播放名入队时解析一次(扫描线程零查库)：排队音未配兜底内置 queue.wav，等待期持续有声
        String voiceName = Optional.ofNullable(iVoiceFileService.getPlayName(skill.getQueueVoice()))
                .orElse(EslConstant.CALL_SKILL_DEFAULT_QUEUE_VOICE);
        String announceVoiceName = iVoiceFileService.getPlayName(skill.getQueueAnnounceVoice());
        callQueues.add(CallQueue.builder().callId(callInfo.getCallId())
                .skillId(callInfo.getSkillId())
                .address(address)
                .type(1)
                .startTime(callInfo.getQueueStartTime())
                .uniqueId(uniqueId)
                .voiceName(voiceName)
                .announceVoiceName(announceVoiceName)
                .lastAnnounceTime(callInfo.getQueueStartTime())
                .playFlag(true)
                .build());
        // 入队即播循环排队音(消除 fsAcd 2s 扫描间隔的静默空窗)
        fsClient.playFile(address, uniqueId, voiceName, 999);
        saveCallInfo(callInfo);
    }

    private void overFlowStrategy(String address, CallInfo callInfo, String uniqueId, CallSkillVo skill) {
        callInfo.setOverflowCount(callInfo.getOverflowCount() == null ? 1 : callInfo.getOverflowCount() + 1);
        // overflow_type 可空(DDL DEFAULT 0)，null 兜底 0=挂机，防拆箱 NPE
        int overflowType = skill.getOverflowType() == null ? 0 : skill.getOverflowType();
        String overflowValue = skill.getOverflowValue();
        // 防环：同一通电话溢出≥2 次强制挂断(A↔B 技能组互溢踢皮球)；
        // 转IVR/转技能组缺目标值(脏数据/绕过前端提交)也按挂机收尾，主叫腿不悬空
        boolean toHangup = callInfo.getOverflowCount() >= 2 || overflowType == 0
                || ((overflowType == 1 || overflowType == 2) && StringUtils.isEmpty(overflowValue));
        if (toHangup) {
            if ((overflowType == 1 || overflowType == 2) && StringUtils.isEmpty(overflowValue)) {
                log.warn("溢出策略目标值为空,按挂机收尾 skillId:{} overflowType:{}", skill.getId(), overflowType);
            }
            callInfo.setHangupDir(3);
            callInfo.setHangupCause(HangupCauseEnum.FULLBUSY.getCode());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            saveCallInfo(callInfo);
            return;
        }
        switch (overflowType) {
            //转IVR
            case 1:
                SpringUtils.getBean(FsIvrRouteHandler.class).handler(address, callInfo, uniqueId, overflowValue);
                return;
            //转技能组：递归走目标组完整逻辑(有空闲直接转，没有则排队/再溢出，overflowCount 防环)
            case 2:
                handler(address, callInfo, uniqueId, overflowValue);
                return;
            //转AI坐席(零配置)：AI 后续转人工走 ai_callbot_config.transfer_skill_id 选组
            case 3:
                SpringUtils.getBean(FsAiRouteHandler.class).handler(address, callInfo, uniqueId, null);
                return;
            default:
                saveCallInfo(callInfo);
        }
    }


    private SipAgentVo getAgentStrategy(CallInfo callInfo, CallSkillVo skill, List<SipAgentStatusVo> freeAgentList) {
        switch (skill.getStrategyType()) {
            //0-随机
            case 0:
                return RandomUtil.randomEle(freeAgentList);
            //1-轮询
            case 1:
                String pollingKey = StringUtils.format(CacheConstants.CALL_SKILL_POLLING_KEY, skill.getId());
                // Number 接收再兜底：Redis 值类型异常时强转/拆箱会炸，对齐显号池轮询的读取模式
                Integer pollingNum = Optional.ofNullable(
                        (Integer) redisService.getCacheObject(pollingKey)).orElse(0);
                // findNextPollingNum 返回的是"下一个轮询 level 值"（存 Redis 供下轮比较），
                // 不是列表下标——按 level 定位成员，原 get(nextPollingNum) 把 level 当下标取人
                Integer nextPollingLevel = findNextPollingNum(freeAgentList, pollingNum);
                redisService.setCacheObject(pollingKey, nextPollingLevel);
                return freeAgentList.stream()
                        .filter(item -> Objects.equals(item.getLevel(), nextPollingLevel))
                        .findFirst().orElse(null);
            //2-最长空闲时间(statusTime 为 null 的脏数据剔除，防拆箱 NPE)
            case 2:
                return freeAgentList.stream().filter(item -> item.getStatusTime() != null)
                        .max(Comparator.comparingLong(item -> DateUtil.current() - item.getStatusTime())).orElse(null);
            //3-当天最少应答次数
            case 3:
                return freeAgentList.stream().min(Comparator.comparing(SipAgentStatusVo::getReceptionNum)).orElse(null);
            //4-最长话后时长(callEndTime 为 null 的脏数据剔除，防拆箱 NPE)
            case 4:
                return freeAgentList.stream().filter(item -> item.getCallEndTime() != null)
                        .min(Comparator.comparingLong(item -> DateUtil.current() - item.getCallEndTime())).orElse(null);
            default:
                return null;
        }
    }




    //acd排队策略
    private void fsAcd() {
        try {
            Set<Long> skillIds = callQueueMap.keySet();
            List<CallQueue> queueList = callQueueMap.values().stream().flatMap(Collection::stream).toList();
            if(CollectionUtil.isEmpty(skillIds) || CollectionUtil.isEmpty(queueList)){
                return;
            }
            CallSkillQuery skillQuery = new CallSkillQuery();
            skillQuery.setIds(new ArrayList<>(skillIds));
            List<CallSkillVo> callSkillList = iCallSkillService.getListByIds(skillQuery);
            if(CollectionUtil.isEmpty(callSkillList)){
                return;
            }
            long current = DateUtil.current();
            callSkillList.sort(Comparator.comparing(CallSkillVo::getPriority, Comparator.reverseOrder()));
            for (CallSkillVo skill : callSkillList) {
                ConcurrentLinkedQueue<CallQueue> callQueues = callQueueMap.get(skill.getId());
                Iterator<CallQueue> iterator = callQueues.iterator();
                while (iterator.hasNext()){
                    CallQueue callQueue = iterator.next();
                    //正常排队超时(timeOut 未配置读系统参数兜底，参数页可调；防拆箱 NPE 炸整轮扫描)
                    int queueTimeoutSec = skill.getTimeOut() == null
                            ? iSysConfigService.getInt(SysConfigKeys.QUEUE_TIMEOUT_DEFAULT, 60)
                            : skill.getTimeOut();
                    if (current/1000 - callQueue.getStartTime()/1000 >= queueTimeoutSec) {
                        callAgentExecutor.execute(() -> {
                            queueTimeout(callQueue);
                        });
                        iterator.remove();
                        continue;
                    }
                    CallInfo callInfo = fsCallCacheService.getCallInfo(callQueue.getCallId());
                    if (callInfo == null) {
                        continue;
                    }

                    List<CallSkillAgentRelVo> agentList = skill.getAgentList();
                    // level 取 null 时 Collectors.toMap 直接 NPE(此处 catch 会吞掉异常炸整轮所有技能组扫描)，兜底 0
                    Map<Long, Integer> skillLevelMap = agentList.stream()
                            .collect(Collectors.toMap(CallSkillAgentRelVo::getAgentId,
                                    m -> m.getLevel() == null ? 0 : m.getLevel(), (key1, key2) -> key1));
                    List<SipAgentStatusVo> freeAgentList = getFreeMembers(agentList, skillLevelMap, callInfo);

                    if(CollectionUtil.isEmpty(freeAgentList)){
                        // 周期播报：配置了播报音且距上次≥间隔(系统参数,参数页可调)，打断循环音播一次；
                        // playFlag 置 false，下轮兜底分支重启循环(FS execute 队列串行，循环会接在播报音后)
                        if (callQueue.getAnnounceVoiceName() != null
                                && current - Optional.ofNullable(callQueue.getLastAnnounceTime()).orElse(0L)
                                >= iSysConfigService.getInt(SysConfigKeys.QUEUE_ANNOUNCE_INTERVAL_MS, EslConstant.CALL_SKILL_ANNOUNCE_INTERVAL_MS)) {
                            fsClient.playBreak(callQueue.getAddress(), callQueue.getUniqueId());
                            fsClient.playFile(callQueue.getAddress(), callQueue.getUniqueId(), callQueue.getAnnounceVoiceName());
                            callQueue.setLastAnnounceTime(current);
                            callQueue.setPlayFlag(false);
                        }
                        if (!Boolean.TRUE.equals(callQueue.getPlayFlag())) {
                            fsClient.playFile(callQueue.getAddress(), callQueue.getUniqueId(), callQueue.getVoiceName(), 999);
                            callQueue.setPlayFlag(true);
                        }
                        continue;
                    }

                    SipAgentVo agentInfo = getAgentStrategy(callInfo, skill, freeAgentList);
                    if (Objects.isNull(agentInfo)) {
                        log.info("转技能组 队列获取空闲坐席失败 callee:{}, skillId:{}", callInfo.getCallee(), skill.getId());
                        if (!Boolean.TRUE.equals(callQueue.getPlayFlag())) {
                            fsClient.playFile(callQueue.getAddress(), callQueue.getUniqueId(), callQueue.getVoiceName(), 999);
                            callQueue.setPlayFlag(true);
                        }
                        continue;
                    }
                    // iterator 必须在迭代线程(fsAcd)内 remove——lambda 跑在 callAgentExecutor 线程，
                    // 跨线程 remove 属误用；且先移除再异步转坐席，防 2s 内下一轮扫描重复分发
                    iterator.remove();
                    callAgentExecutor.execute(() -> {
                        callInfo.setQueueEndTime(current);
                        // 排队音打断与转坐席提示音统一在 transferAgentHandler 入口处理
                        transferAgentHandler(callQueue.getAddress(),agentInfo, callInfo, callQueue.getUniqueId(), skill);
                    });
                }
                //callQueueMap.remove(skill.getId());
            }
        } catch (Exception e) {
            log.error("排队异常 error:{}",e.getMessage(),e);
        }
    }

    private void transferAgentHandler(String address, SipAgentVo agentInfo, CallInfo callInfo, String uniqueId, CallSkillVo skill) {
        String agentNumber = agentInfo.getAgentNumber();
        String otherUniqueId = RandomUtil.randomNumbers(32);

        if (StringUtils.isEmpty(agentNumber)) {
            log.error("transferAgentHandler agent:{} 坐席未绑定SIP号码, callId:{}", agentInfo.getId(), callInfo.getCallId());
            callInfo.setSkillHangUpReason(HangupCauseEnum.AGENT_NO_BAND_SIP.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        CallRouteVo callRoute = fsCallCacheService.getCallRoute(callInfo.getCallee(), callInfo.getCaller(), DirectionEnum.INBOUND.getType());
        if(Objects.isNull(callRoute)){
            log.info("transferAgentHandler 未配置号码路由 callee:{}",agentNumber);
            callInfo.setSkillHangUpReason(HangupCauseEnum.NOT_ROUTE.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        FsSipGatewayQuery query = new FsSipGatewayQuery();
        query.setGatewayType(0);
        List<FsSipGateway> gatewayList = iFsSipGatewayService.getList(query);
        if(CollectionUtil.isEmpty(gatewayList)){
            log.info("transferAgentHandler 号码路由未关联网关信息 callee:{}",agentNumber);
            callInfo.setSkillHangUpReason(HangupCauseEnum.ROUTE_NOT_GATEWAY.getDesc());
            fsClient.hangupCall(address, callInfo.getCallId(), uniqueId);
            return;
        }
        callInfo.setAgentId(agentInfo.getId());
        callInfo.setAgentNumber(agentInfo.getAgentNumber());
        callInfo.setAgentName(agentInfo.getName());
        // 1.11 坐席分配后同步更新 CallRecord，保证通话中 ws 推送能定位坐席
        updateCallRecordAgent(callInfo);
        callInfo.setCallee(agentInfo.getAgentNumber());
        // 转坐席提示音(覆盖直转+排队分配两场景)：打断排队循环音/播报，配了 agent_voice
        // 播一次再 originate(未配静默转接)；break 对未在播放的通道是无害空操作
        fsClient.playBreak(address, uniqueId);
        String agentVoiceName = iVoiceFileService.getPlayName(skill.getAgentVoice());
        if (StringUtils.isNotEmpty(agentVoiceName)) {
            fsClient.playFile(address, uniqueId, agentVoiceName);
        }
        //构建被叫(坐席)腿
        ChannelInfo otherChannelInfo = ChannelInfo.builder().callId(callInfo.getCallId()).uniqueId(otherUniqueId).cdrType(2).type(2).directionType(2)
                .agentId(agentInfo.getId()).agentNumber(agentInfo.getAgentNumber()).agentName(agentInfo.getName())
                .callTime(DateUtil.current()).otherUniqueId(uniqueId)
                .called(agentNumber).caller(callInfo.getCaller()).display(callInfo.getCallerDisplay()).build();
        callInfo.setChannelInfoMap(otherUniqueId,otherChannelInfo);
        bindMutualOtherUniqueId(callInfo, uniqueId, otherUniqueId);
        callInfo.setProcess(ProcessEnum.CALL_BRIDGE);
        // 先落缓存再 originate：坐席腿事件若先于缓存写入到达会被各 handler 静默丢弃
        fsCallCacheService.saveCallInfo(callInfo);
        fsCallCacheService.saveCallRel(otherUniqueId,callInfo.getCallId());


        fsClient.makeCall(address,callInfo.getCallId(), callInfo.getCallee(),callInfo.getCallerDisplay(),otherUniqueId,callInfo.getCalleeTimeOut(), gatewayList.get(0));

        Boolean isAgent = redisService.getCacheMapHasKey(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()));
        if(Boolean.TRUE.equals(isAgent)){
            SipAgentStatusVo agentStatusVo = redisService.getCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()));
            agentStatusVo.setReceptionNum(agentStatusVo.getReceptionNum() + 1);
            // 写回键与读取键统一用 agentId(原写 vo.getId()，两者不等时更新错条目)
            redisService.setCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(callInfo.getAgentId()), agentStatusVo);
        }
    }

    /**
     * 坐席腿未接通挂断(漏接)重分配：同步事件(FsChannelHangUpComplete 发布，ESL 事件线程执行)。
     * 直转链路(flowDataContext 为空)由此处理，IVR 链路由 FlowSkillGroupRouteHandler 处理。
     */
    @EventListener
    public void onAgentMissed(SkillAgentMissedEvent event) {
        CallInfo callInfo = fsCallCacheService.getCallInfo(event.getCallId());
        if (callInfo == null || callInfo.getFlowDataContext() != null) {
            return;
        }
        int retryCount = Optional.ofNullable(callInfo.getAgentRetryCount()).orElse(0);
        callInfo.setAgentRetryCount(retryCount + 1);
        // 重分配次数上限读系统参数(参数页可调,默认 1；0=漏接不换人直接走溢出收尾)
        if (retryCount >= iSysConfigService.getInt(SysConfigKeys.MISSED_REASSIGN_LIMIT, 1)) {
            // 已达上限仍漏接：不再分配，按溢出策略收尾
            log.info("坐席漏接重分配已达上限,走溢出策略 callId:{}", callInfo.getCallId());
            CallSkillVo skill = iCallSkillService.getDetail(callInfo.getSkillId());
            if (skill == null) {
                callInfo.setSkillHangUpReason(HangupCauseEnum.FULLBUSY.getDesc());
                fsClient.hangupCall(event.getAddress(), callInfo.getCallId(), event.getCallerUniqueId());
                saveCallInfo(callInfo);
                return;
            }
            overFlowStrategy(event.getAddress(), callInfo, event.getCallerUniqueId(), skill);
            return;
        }
        log.info("坐席漏接,重新分配 callId:{} 排除坐席:{}", callInfo.getCallId(), callInfo.getLastFailedAgentId());
        // 恢复原始被叫(上次 transferAgentHandler 已把 callee 覆盖成坐席号码，号码路由查询依赖原被叫)
        ChannelInfo callerLeg = callInfo.getChannelMap().get(event.getCallerUniqueId());
        if (callerLeg != null && StringUtils.isNotEmpty(callerLeg.getCalled())) {
            callInfo.setCallee(callerLeg.getCalled());
        }
        handler(event.getAddress(), callInfo, event.getCallerUniqueId(), String.valueOf(callInfo.getSkillId()));
    }

    /**
     * 超时挂机
     * @param callQueue
     */
    private void queueTimeout(CallQueue callQueue) {
        fsClient.playBreak(callQueue.getAddress(), callQueue.getUniqueId());
        Long current = DateUtil.current();
        CallInfo callInfo = fsCallCacheService.getCallInfo(callQueue.getCallId());
        if (callInfo == null) {
            return;
        }
        callInfo.setQueueEndTime(current);
        log.info("排队超时 callId:{} queueTimeout:{}", callQueue.getCallId(), callQueue.getStartTime());
        //排队超时挂机
        callInfo.setHangupDir(3);
        callInfo.setHangupCause(HangupCauseEnum.QUEUE_TIME_OUT.getCode());
        if (!CollectionUtils.isEmpty(callInfo.getDetailList())) {
            CallInfoDetail callDetail = callInfo.getDetailList().get(callInfo.getDetailList().size() - 1);
            if (callDetail != null) {
                callDetail.setEndTime(current);
            }
        }
        callInfo.setSkillHangUpReason(HangupCauseEnum.QUEUE_TIME_OUT.getDesc());

        fsClient.hangupCall(callQueue.getAddress(), callQueue.getCallId(), callQueue.getUniqueId());
        saveCallInfo(callInfo);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("开启呼入队列扫描");
        fsAcdThread.scheduleAtFixedRate(this::fsAcd, 5, 2, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() throws Exception {
        log.info("关闭呼入队列扫描");
        fsAcdThread.shutdown();
    }
    /**
     * 二分查询轮训级别
     *
     * @param freeAgentList
     * @param pollingNum
     * @return
     */
   private Integer findNextPollingNum(List<SipAgentStatusVo> freeAgentList, int pollingNum) {
        freeAgentList.sort(Comparator.comparing(SipAgentStatusVo::getLevel));
        int left = 0;
        int right = freeAgentList.size() - 1;
        if(pollingNum >= freeAgentList.get(right).getLevel()){
            return freeAgentList.get(left).getLevel();
        }
        while (left < right) {
            //防止整数溢出
            int mid = (right - left) / 2 + left;
            //如果当前元素比目标元素小或者相同 则去他右边找
            if (freeAgentList.get(mid).getLevel() <= pollingNum) {
                left = mid + 1;
            } else {
                //如果当前元素比目标元素大 则去他左边找 此时注意 应该包括其当前边界
                right = mid;
            }
        }
        return freeAgentList.get(left).getLevel();
    }



}
