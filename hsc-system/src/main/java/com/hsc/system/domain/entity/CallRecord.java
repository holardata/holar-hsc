// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;


/**
 * 呼叫记录表(CallRecord)表实体类
 *
 * @author danmo
 * @since 2024-11-21 11:23:02
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("call_record")
public class CallRecord extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 373517321891756184L;

    /**
     * 主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 呼叫唯一ID
     */
    @Schema(description = "呼叫唯一ID")
    @TableField("call_id")
    private String callId;


    /**
     * 主叫号码
     */
    @Schema(description = "主叫号码")
    @TableField("caller_number")
    private String callerNumber;


    /**
     * 主叫显号
     */
    @Schema(description = "主叫显号")
    @TableField("caller_display_number")
    private String callerDisplayNumber;


    /**
     * 被叫号码
     */
    @Schema(description = "被叫号码")
    @TableField("callee_number")
    private String calleeNumber;


    /**
     * 被叫显号
     */
    @Schema(description = "被叫显号")
    @TableField("callee_display_number")
    private String calleeDisplayNumber;


    /**
     * 号码归属地
     */
    @Schema(description = "号码归属地")
    @TableField("number_location")
    private String numberLocation;


    /**
     * 坐席ID
     */
    @Schema(description = "坐席ID")
    @TableField("agent_id")
    private Long agentId;


    /**
     * 坐席号码
     */
    @Schema(description = "坐席号码")
    @TableField("agent_number")
    private String agentNumber;


    /**
     * 坐席名称
     */
    @Schema(description = "坐席名称")
    @TableField("agent_name")
    private String agentName;


    /**
     * 呼叫状态 1-成功 2-失败
     */
    @Schema(description = "呼叫状态 1-成功 2-失败")
    @TableField("call_state")
    private Integer callState;


    /**
     * 呼叫方式 1-呼出 2-呼入
     */
    @Schema(description = "呼叫方式 1-呼出 2-呼入")
    @TableField("direction")
    private Integer direction;


    /**
     * 呼叫开始时间
     */
    @Schema(description = "呼叫开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("call_start_time")
    private Date callStartTime;


    /**
     * 呼叫结束时间
     */
    @Schema(description = "呼叫结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("call_end_time")
    private Date callEndTime;


    /**
     * 应答标识 0-接通 1-坐席未接用户未接 2-坐席接通用户未接通 3-用户接通坐席未接通
     */
    @Schema(description = "应答标识 0-接通 1-坐席未接用户未接 2-坐席接通用户未接通 3-用户接通坐席未接通")
    @TableField("answer_flag")
    private Integer answerFlag;


    /**
     * 呼叫接通时间
     */
    @Schema(description = "呼叫接通时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("answer_time")
    private Date answerTime;


    /**
     * 振铃时间
     */
    @Schema(description = "振铃时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("ringing_time")
    private Date ringingTime;

    /**
     * 首次接通时刻（最早一条腿 bridge，真人/AI 接上；详情"接通耗时"=本字段-呼叫开始时间）
     */
    @Schema(description = "首次接通时刻(最早一条腿bridge)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("bridge_time")
    private Date bridgeTime;


    /**
     * 挂机方向 1-主叫挂机 2-被叫挂机 3-系统挂机
     */
    @Schema(description = "挂机方向 1-主叫挂机 2-被叫挂机 3-系统挂机")
    @TableField("hangup_dir")
    private Integer hangupDir;


    /**
     * 挂机原因
     */
    @Schema(description = "挂机原因 ")
    @TableField("hangup_cause_code")
    private Integer hangupCauseCode;


    /**
     * 录音文件地址
     */
    @Schema(description = "录音文件地址")
    @TableField("file_path")
    private String filePath;


    /**
     * 振铃文件地址
     */
    @Schema(description = "振铃文件地址")
    @TableField("ringing_path")
    private String ringingPath;


    /**
     * 是否AI先接听 0-否 1-是
     */
    @Schema(description = "是否AI先接听 0-否 1-是")
    @TableField("is_ai_first")
    private Integer isAiFirst;


    /**
     * AI转人工时间
     */
    @Schema(description = "AI转人工时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("transfer_time")
    private Date transferTime;

    /**
     * 是否转人工 0-否 1-是（仅 AI 智能坐席接听且成功转人工时为 1，其余默认 0）
     */
    @Schema(description = "是否转人工 0-否 1-是")
    @TableField("transfer_human")
    private Integer transferHuman;


    /**
     * 命中呼叫路由ID(关联 call_route.id)
     */
    @Schema(description = "命中呼叫路由ID(关联 call_route.id)")
    @TableField("route_id")
    private Long routeId;


    public void transfer(CallInfo callInfo) {
        this.setCallId(String.valueOf(callInfo.getCallId()));
        this.setAgentId(callInfo.getAgentId());
        this.setAgentNumber(callInfo.getAgentNumber());
        this.setAgentName(callInfo.getAgentName());
        this.setCallerNumber(callInfo.getCaller());
        this.setCallerDisplayNumber(callInfo.getCallerDisplay());
        this.setCalleeNumber(callInfo.getCallee());
        this.setCalleeDisplayNumber(callInfo.getCalleeDisplay());
        this.setNumberLocation(callInfo.getNumberLocation());
        if(Objects.nonNull(callInfo.getCallTime())){
            this.setCallStartTime(new Date(callInfo.getCallTime()));
        }
        this.setCallEndTime(new Date());

        this.setDirection(mapDirectionFromCallInfo(callInfo.getDirection()));
        // answerFlag：真正接通(answerTime 来自 CHANNEL_BRIDGE)=0；未接通按方向——
        // 外呼(callInfo.direction=2,客户没接;晚应答下坐席也未answer)=1(坐席用户均未接)；
        // 呼入(direction=1,坐席没接)=3(用户接通坐席未接通)
        this.setAnswerFlag(Objects.nonNull(callInfo.getAnswerTime()) ? 0
                : (Objects.equals(callInfo.getDirection(), 2) ? 1 : 3));
        if (Objects.nonNull(callInfo.getAnswerTime())) {
            this.setAnswerTime(new Date(callInfo.getAnswerTime()));
        }

        if (Objects.nonNull(callInfo.getCalleeRingStartTime())) {
            this.setRingingTime(new Date(callInfo.getCalleeRingStartTime()));
        }
        // 首次接通时刻 = 各腿最早的 bridgeTime（真人/AI 接上；主叫与被叫腿各有 bridgeTime，取最早）
        callInfo.getChannelMap().values().stream()
                .map(ChannelInfo::getBridgeTime)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .ifPresent(t -> this.setBridgeTime(new Date(t)));

        this.setHangupDir(callInfo.getHangupDir());
        this.setHangupCauseCode(callInfo.getHangupCause());
        this.setFilePath(callInfo.getRecord());
        this.setRouteId(callInfo.getRouteId());
        // callState 反映"是否真正接通"，而非仅看挂断原因：
        // 未接通(answerFlag≠0)的通话，即使 hangupCause=正常挂机(NORMAL_CLEARING)，
        // 也不应记为"成功"——否则"外呼没接通就挂"会因 cause 正常而误显成功。
        // answerFlag=0 表示接通(CHANNEL_BRIDGE 才置 0)，其余均为未接通。
        this.setCallState(Objects.equals(this.getAnswerFlag(), 0) ? 1 : 2);
        //this.setRingingPath(callInfo.getRecord());

    }

    /**
     * call_record.direction 持久化语义为 1-呼出 2-呼入，
     * 与 CallInfo/DirectionEnum 的 1-呼入 2-外呼 相反。
     * CallInfo → CallRecord 落库时必须翻转，集中在此供各落库点统一调用。
     */
    public static Integer mapDirectionFromCallInfo(Integer callInfoDirection) {
        if (callInfoDirection == null) {
            return null;
        }
        return Objects.equals(callInfoDirection, 1) ? 2 : 1;
    }
}

