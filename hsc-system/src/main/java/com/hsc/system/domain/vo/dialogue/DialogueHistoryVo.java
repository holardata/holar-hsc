package com.hsc.system.domain.vo.dialogue;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 通话对话历史视图（平迁自 reminder_backend HistoryRecord 列表展示字段）。
 *
 * <p>由 CallRecord(通话) + CallAiSummary(摘要状态) + dialog_record 句数统计 拼装。
 */
@EqualsAndHashCode(callSuper = true)
@Schema
@Data
public class DialogueHistoryVo extends BaseVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "通话ID")
    private String callId;

    @Schema(description = "主叫号码")
    private String callerNumber;

    @Schema(description = "被叫号码")
    private String calleeNumber;

    @Schema(description = "坐席工号")
    private String agentNumber;

    @Schema(description = "坐席名称")
    private String agentName;

    @Schema(description = "呼叫方式 1-呼出 2-呼入")
    private Integer direction;

    @Schema(description = "呼叫状态 1-成功 2-失败")
    private Integer callState;

    @Schema(description = "是否转人工 0-否 1-是")
    private Integer transferHuman;

    @Schema(description = "呼叫开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date callStartTime;

    @Schema(description = "呼叫结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date callEndTime;

    @Schema(description = "通话时长(秒)")
    private Long duration;

    @Schema(description = "录音文件地址")
    private String filePath;

    @Schema(description = "呼叫路由名称")
    private String routeName;

    /** 所属任务名（经拨打历史反查；手拨/呼入为空，见设计 D15——话单不存业务标识） */
    private String taskName;

    /** 客户名（经拨打历史反查 customer_seas.name；无档案为空） */
    private String customerName;

    @Schema(description = "客户ASR句数")
    private Long userTxtNum;

    @Schema(description = "AI回复句数")
    private Long aiTxtNum;

    @Schema(description = "全文总结状态 0-未请求 1-处理中 2-已完成")
    private Integer summaryStatus;

    @Schema(description = "关键词状态")
    private Integer keywordStatus;

    @Schema(description = "角色总结状态")
    private Integer roleStatus;

    @Schema(description = "要点提炼状态")
    private Integer keypointStatus;

    @Schema(description = "代办事项状态")
    private Integer todoStatus;

    @Schema(description = "思维导图状态")
    private Integer xmindStatus;

    @Schema(description = "IVR满意度评分 1-5(0-未评价，null-无IVR满意度节点)")
    private Integer satisfactionScore;

    /** 主叫显号 */
    private String callerDisplayNumber;

    /** 被叫显号 */
    private String calleeDisplayNumber;

    /** 号码归属地 */
    private String numberLocation;

    /** 路由ID（详情层按它解析路由类型/目标/日程） */
    private Long routeId;

    /** 路由类型名：坐席/外呼/SIP/技能组/放音/IVR/座机/AI智能坐席 */
    private String routeTypeName;

    /** 路由目标名：按类型解析——IVR流程名/技能组名/坐席名/网关名/语音文件名/SIP地址/座机分机号 */
    private String routeTargetName;

    /** 生效日程名 */
    private String scheduleName;

    /** 挂断方向：1-主叫挂断 2-被叫挂断 */
    private Integer hangupDir;

    /** 挂断原因码（Q.850） */
    private Integer hangupCauseCode;

    /** 挂断原因文案（FsHangupCauseEnum desc） */
    private String hangupCauseName;

    /** 振铃时间（被叫侧开始振铃：呼出=客户手机振铃；呼入=坐席话机振铃） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ringingTime;

    /** 接听时间（首次接通时刻：最早一条腿 bridge，真人/AI 接上） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date bridgeTime;

    /** 接通耗时（秒）：呼入开始到真人/AI 接上（bridgeTime - callStartTime），未接通为 null */
    private Long connectSeconds;

    /** 是否AI先接听：1-是 */
    private Integer isAiFirst;

    /** AI 转人工时间点 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date transferTime;

    /** 客户（对端号码）历史来电次数（含本通）：caller_number=对端 且 direction=呼入 */
    private Long inboundCount;

    /** 历史去电次数（我们打给该客户，含本通）：callee_number=对端 且 direction=呼出 */
    private Long outboundCount;
}
