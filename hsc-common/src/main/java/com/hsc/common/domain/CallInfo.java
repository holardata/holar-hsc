// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.domain;

import cn.hutool.core.collection.CollectionUtil;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.enums.RouteTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 呼叫对象
 *
 * @author danmo
 * @date 2023-10-23 15:16
 **/
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CallInfo {

    /**
     * 呼叫唯一ID
     */
    private Long callId;

    /**
     * 呼叫通道ID列表
     */
    private List<String> uniqueIdList;

    /**
     * 主叫号码
     */
    private String caller;

    /**
     * 主叫显号
     */
    private String callerDisplay;

    /**
     * 被叫号码
     */
    private String callee;

    /**
     * 被叫显号
     */
    private String calleeDisplay;

    /**
     * 号码归属地
     */
    private String numberLocation;

    /**
     * 坐席ID
     */
    private Long agentId;

    /**
     * 任务联系人ID（call_task_assignment.id）：任务拨打经 SIP 头/代拨入参携带，非任务呼叫为 null
     */
    private Long assignmentId;

    /**
     * 任务ID（call_task.id）：任务拨打时随 assignment 一并携带，非任务呼叫为 null
     */
    private Long taskId;

    /**
     * 客户ID（customer_seas.id）：任务/私海拨打携带（无档案为 null），手拨/呼入为 null
     */
    private Long customerId;

    /**
     * 坐席号码
     */
    private String agentNumber;

    /**
     * 坐席名称
     */
    private String agentName;


    /**
     * 呼叫方向(1-呼入 2-外呼)
     */
    private Integer direction;

    /**
     * 路由类型
     */
    private Integer routeType;

    /**
     * 命中呼叫路由ID（路由分发时注入；坐席互打为 null）
     */
    private Long routeId;

    /**
     * 开始呼叫时间
     */
    private Long callTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 是否隐藏客户号码(0-不隐藏 1-隐藏)
     */
    @Builder.Default
    private Integer hiddenCustomer = 0;

    /**
     * 主叫超时时间
     */
    @Builder.Default
    private Integer callerTimeOut = 10;


    /**
     * 被叫超时时间
     */
    @Builder.Default
    private Integer calleeTimeOut = 10;


    /**
     * 话单通知地址
     */
    private String cdrNotifyUrl;

    /**
     * 1主叫挂机, 2:被叫挂机, 3:平台挂机
     */
    private Integer hangupDir;

    /**
     * 平台挂机原因
     */
    private Integer hangupCause;

    /**
     * 主叫振铃开始时间
     */
    private Long callerRingStartTime;
    /**
     * 主叫振铃结束时间
     */
    private Long callerRingEndTime;

    /**
     * 被叫振铃开始时间
     */
    private Long calleeRingStartTime;
    /**
     * 被叫振铃结束时间
     */
    private Long calleeRingEndTime;

    /**
     * 接听时间
     */
    private Long answerTime;

    /**
     * 录音开始时间
     */
    private Long recordStartTime;

    /**
     * 录音结束时间
     */
    private Long recordEndTime;

    /**
     * 录音时长
     */
    private Long recordTime;

    /**
     * 录音地址
     */
    private String record;

    /**
     * AI 转人工候选目标 JSON（[{number,name,agentId,landline}]，重试用）
     */
    private String transferTargets;

    /**
     * AI 转人工当前 originate 索引
     */
    @Builder.Default
    private Integer transferIndex = 0;

    /**
     * AI 转人工 originate 进行中（坐席腿 originate 后置 true，bridge 成功/失败后清 false）
     */
    @Builder.Default
    private Boolean aiTransferring = false;

    /**
     * 结束语音播完挂断标记（IVR 结束节点配了挂机前语音时置 true；
     * playback 完成事件里查此标记挂断——hangup app 到达 FS 即生效不等排队，不能与 playback 同批下发）
     */
    @Builder.Default
    private Boolean endPlaybackHangup = false;

    /**
     * 应答数
     */
    @Builder.Default
    private Integer answerCount = 0;

    /**
     * 技能组ID
     */
    private Long skillId;

    /**
     * 第一次进队列时间
     */
    private Long firstQueueTime;

    /**
     * 进入技能组时间
     */
    private Long queueStartTime;

    /**
     * 出技能组时间
     */
    private Long queueEndTime;

    /**
     * 溢出次数
     */
    private Integer overflowCount;

    /**
     * 技能组漏接坐席ID（内存态不落库：坐席腿未接通挂断时记录，重分配选人时本通内排除）
     */
    private Long lastFailedAgentId;

    /**
     * 漏接重分配次数（内存态不落库：≥1 次后不再重分配、走溢出策略收尾）
     */
    private Integer agentRetryCount;

    /**
     * 呼叫通道信息
     */
    private Map<String, ChannelInfo> channelMap;

    /**
     * 电话流转详情
     */
    private List<CallInfoDetail> detailList;

    /**
     * 执行任务
     */
    private ProcessEnum process;

    /**
     * 流程数据
     */
    private FlowDataContext flowDataContext;

    public void setChannelInfoMap(String uniqueId, ChannelInfo channelInfo) {
        if (CollectionUtil.isEmpty(channelMap)) {
            channelMap = new HashMap<>();
        }
        channelMap.put(uniqueId, channelInfo);
    }

    public void removeChannelInfoMap(String uniqueId) {
        if (CollectionUtil.isNotEmpty(channelMap)) {
            channelMap.remove(uniqueId);
        }
    }


    public void addUniqueIdList(String uniqueId) {
        if (CollectionUtil.isEmpty(uniqueIdList)) {
            this.uniqueIdList = new ArrayList<>();
        }
        this.uniqueIdList.add(uniqueId);
    }

    public void removeUniqueIdList(String uniqueId) {
        if (CollectionUtil.isNotEmpty(uniqueIdList)) {
            uniqueIdList.remove(uniqueId);
        }
    }

    public void addDetailList(CallInfoDetail detail) {
        if (CollectionUtil.isEmpty(detailList)) {
            this.detailList = new ArrayList<>();
        }
        this.detailList.add(detail);
    }

    public void setSkillHangUpReason(String reason) {
        if (CollectionUtil.isEmpty(detailList)) {
            return;
        }
        for (CallInfoDetail detail : detailList) {
            if (Objects.equals(3, detail.getTransferType())) {
                detail.setReason(reason);
                return;
            }
        }
    }
}
