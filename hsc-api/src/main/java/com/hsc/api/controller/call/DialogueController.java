package com.hsc.api.controller.call;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IIvrSatisfactionRecordService;
import com.hsc.ivr.domain.vo.FlowInfoVo;
import com.hsc.system.domain.entity.AbstractRecord;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.CallRoute;
import com.hsc.system.domain.entity.CallSchedule;
import com.hsc.system.domain.entity.CallSkill;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.dialogue.AbstractRecordEditQuery;
import com.hsc.system.domain.query.dialogue.CallAiSummaryEditQuery;
import com.hsc.system.domain.query.dialogue.DialogueHistoryQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.calltask.domain.entity.CallTask;
import com.hsc.calltask.domain.entity.CallTaskDialLog;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.service.ICallTaskDialLogService;
import com.hsc.calltask.service.ICallTaskService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.vo.dialogue.DialogueHistoryVo;
import com.hsc.system.domain.vo.dialogue.FeedbackStatsVo;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.service.ICallRecordService;
import com.hsc.system.service.ICallRouteService;
import com.hsc.system.service.ICallScheduleService;
import com.hsc.system.service.ICallSkillService;
import com.hsc.system.service.IDialogueService;
import com.hsc.system.service.IFsSipGatewayService;
import com.hsc.system.service.ISipAgentService;
import com.hsc.system.service.IVoiceFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通话对话管理（平迁自 reminder_backend DialogueController）。
 *
 * <p>对话历史分页 / 实时转写与过程摘要查询 / 翻译 / AI改写 / 内容替换 /
 * 摘要与过程摘要人工编辑 / 单字段重算 / 发言人修改 / 反馈 / 删除。
 *
 * <p>入参统一用 callId（hsc 关联键，对应 rb 的 sessionId）；单条编辑用各表主键 id。
 * 业务编排见 {@link IDialogueService}。
 */
@Tag(name = "通话对话管理")
@RestController
@Slf4j
@RequestMapping("/system/v1/dialogue")
public class DialogueController extends BaseController {

    @Autowired
    private IDialogueService iDialogueService;

    @Autowired
    private IIvrSatisfactionRecordService ivrSatisfactionRecordService;

    /** 路由目标/日程名解析（详情 fillRouteInfo 用；flow_info 在 hsc-ivr，须在本层拼装） */
    @Autowired
    private ICallRouteService iCallRouteService;

    @Autowired
    private ICallScheduleService iCallScheduleService;

    @Autowired
    private ISipAgentService iSipAgentService;

    @Autowired
    private ICallSkillService iCallSkillService;

    @Autowired
    private IFsSipGatewayService iFsSipGatewayService;

    @Autowired
    private IVoiceFileService iVoiceFileService;

    @Autowired
    private IFlowInfoService iFlowInfoService;

    /** 客户来去电统计（详情 fillContactStats 用） */
    @Autowired
    private ICallRecordService callRecordService;

    /** 话单"所属任务/客户"反查（fillTaskInfo 用，设计 D15：hsc-system 不存业务标识，api 层拼装） */
    @Autowired
    private ICallTaskDialLogService iCallTaskDialLogService;

    @Autowired
    private ICallTaskService iCallTaskService;

    @Autowired
    private ICustomerSeasService iCustomerSeasService;

    // ============================== 查询 ==============================
    // 以下 5 个查询接口（history/page 除外）均不加 @PreAuthorize——走 SecurityConfig 默认
    // authenticated 兜底（登录即可查，不限按钮权限）：软电话/座机通话记录跳话单详情的坐席
    // 未必有「通话记录」菜单权限（system:dialogue:list 挂在该菜单 F 按钮下），与 record/play 同理。
    // history/page 是管理侧话单列表，保留 perms；编辑/删除类接口同样保留。

    @Log(title = "对话历史分页", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:dialogue:list')")
    @Operation(summary = "对话历史分页(通话记录+摘要状态+句数统计+IVR满意度)", method = "POST")
    @RequestMapping(value = "/history/page", method = RequestMethod.POST)
    public ResResult<PageInfo<DialogueHistoryVo>> historyPage(@RequestBody DialogueHistoryQuery query) {
        PageInfo<DialogueHistoryVo> page = iDialogueService.historyPage(query);
        fillSatisfaction(page.getList());
        fillTaskInfo(page.getList());
        return success(page);
    }

    /**
     * 回填 IVR 满意度评分（一次 IN 批量查，避免逐条 N+1）。
     * 满意度数据在 hsc-ivr 域，回填放 Controller 层——hsc-system 不可反向依赖 hsc-ivr。
     */
    private void fillSatisfaction(List<DialogueHistoryVo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        List<String> callIds = list.stream()
                .map(DialogueHistoryVo::getCallId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (callIds.isEmpty()) {
            return;
        }
        Map<String, Integer> scoreMap = ivrSatisfactionRecordService.getLatestScoreByCallIds(callIds);
        list.forEach(vo -> vo.setSatisfactionScore(scoreMap.get(vo.getCallId())));
    }

    /**
     * 回填话单的"所属任务/客户"（设计 D15：话单表不存业务标识，经拨打历史 dial_log 按 call_id
     * 反查三段式：dial_log IN → 任务名/客户名各一次 IN，无 N+1）。手拨/呼入话单无对应记录，两列为空。
     */
    private void fillTaskInfo(List<DialogueHistoryVo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        List<String> callIds = list.stream()
                .map(DialogueHistoryVo::getCallId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (callIds.isEmpty()) {
            return;
        }
        List<CallTaskDialLog> dialLogs = iCallTaskDialLogService.list(new LambdaQueryWrapper<CallTaskDialLog>()
                .in(CallTaskDialLog::getCallId, callIds)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (CollectionUtil.isEmpty(dialLogs)) {
            return;
        }
        // 同一 callId 只有一条拨历史（一通电话一次拨打）；任务名/客户名各一次 IN 查拼 Map
        Map<String, CallTaskDialLog> byCallId = dialLogs.stream()
                .collect(Collectors.toMap(CallTaskDialLog::getCallId, l -> l, (a, b) -> a));
        Set<Long> taskIds = dialLogs.stream().map(CallTaskDialLog::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> taskNames = taskIds.isEmpty() ? Collections.emptyMap()
                : iCallTaskService.listByIds(taskIds).stream()
                        .collect(Collectors.toMap(CallTask::getId, CallTask::getName, (a, b) -> a));
        Set<Long> customerIds = dialLogs.stream().map(CallTaskDialLog::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerIds.isEmpty() ? Map.of()
                : iCustomerSeasService.listByIds(customerIds).stream()
                        .filter(c -> StringUtils.isNotBlank(c.getPhone()))
                        .collect(Collectors.toMap(CustomerSeas::getId, c -> StringUtils.isBlank(c.getName()) ? c.getPhone() : c.getName(), (a, b) -> a));
        list.forEach(vo -> {
            CallTaskDialLog dialLog = byCallId.get(vo.getCallId());
            if (dialLog == null) {
                return;
            }
            if (dialLog.getTaskId() != null) {
                vo.setTaskName(taskNames.get(dialLog.getTaskId()));
            } else {
                vo.setTaskName("私海拨打");
            }
            if (dialLog.getCustomerId() != null) {
                vo.setCustomerName(customerNames.get(dialLog.getCustomerId()));
            }
        });
    }

    @Log(title = "话单详情", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按callId查话单详情(基础字段+挂断/接通/路由目标/满意度/客户来电统计)", method = "GET")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public ResResult<DialogueHistoryVo> info(@RequestParam("callId") String callId) {
        DialogueHistoryVo vo = iDialogueService.info(callId);
        fillSatisfaction(List.of(vo));
        fillTaskInfo(List.of(vo));
        fillRouteInfo(vo);
        fillContactStats(vo);
        return success(vo);
    }

    /**
     * 回填客户（对端号码）历史来去电次数（详情页"服务过程"用，以号码为维度）。
     * 对端号码：呼入=主叫、呼出=被叫；计数含本通。号码归属天然局限（客户换号对不上）。
     */
    private void fillContactStats(DialogueHistoryVo vo) {
        if (vo == null) {
            return;
        }
        // 呼入对端=主叫(2=呼入,对齐 CallDirectionEnum)；呼出对端=被叫
        String peer = Objects.equals(vo.getDirection(), 2)
                ? vo.getCallerNumber() : vo.getCalleeNumber();
        if (peer == null || peer.isBlank()) {
            return;
        }
        vo.setInboundCount(callRecordService.count(new LambdaQueryWrapper<CallRecord>()
                .eq(CallRecord::getCallerNumber, peer)
                .eq(CallRecord::getDirection, 2)));
        vo.setOutboundCount(callRecordService.count(new LambdaQueryWrapper<CallRecord>()
                .eq(CallRecord::getCalleeNumber, peer)
                .eq(CallRecord::getDirection, 1)));
    }

    /**
     * 回填路由类型/目标名/日程名（详情页"路由信息"分组用）。
     * 解析放 Controller 层——flow_info 实体在 hsc-ivr 域，hsc-system 不可反向依赖
     * （同 fillSatisfaction 的分层理由）。目标配置被删时名称显示为空，不阻断详情。
     */
    private void fillRouteInfo(DialogueHistoryVo vo) {
        if (vo == null || vo.getRouteId() == null) {
            return;
        }
        try {
            CallRoute route = iCallRouteService.getById(vo.getRouteId());
            if (route == null || route.getRouteType() == null) {
                return;
            }
            vo.setRouteTypeName(routeTypeName(route.getRouteType()));
            vo.setRouteTargetName(resolveRouteTarget(route.getRouteType(), route.getRouteValue()));
            if (route.getScheduleId() != null) {
                CallSchedule schedule = iCallScheduleService.getById(route.getScheduleId());
                if (schedule != null) {
                    vo.setScheduleName(schedule.getName());
                }
            }
        } catch (Exception e) {
            // 路由目标解析失败（配置脏数据等）不影响详情主体
            log.warn("话单详情路由目标解析失败 callId:{}, routeId:{}", vo.getCallId(), vo.getRouteId(), e);
        }
    }

    /** 路由类型 → 业务文案（对齐 RouteTypeEnum 1-8，AI 用统一叫法） */
    private String routeTypeName(Integer routeType) {
        return switch (routeType) {
            case 1 -> "坐席";
            case 2 -> "外呼";
            case 3 -> "SIP";
            case 4 -> "技能组";
            case 5 -> "放音";
            case 6 -> "IVR";
            case 7 -> "座机";
            case 8 -> "AI智能坐席";
            default -> String.valueOf(routeType);
        };
    }

    /**
     * 路由目标名：按类型查对应配置表（IVR→流程名、技能组→组名、坐席→坐席名、外呼→网关名、
     * 放音→语音文件名）；SIP/座机的 routeValue 本身就是地址/分机号，直接展示；AI 无具体目标。
     */
    private String resolveRouteTarget(Integer routeType, String routeValue) {
        if (routeValue == null || routeValue.isBlank()) {
            return null;
        }
        return switch (routeType) {
            case 1 -> { // 坐席
                SipAgentVo agent = iSipAgentService.getDetail(Long.valueOf(routeValue));
                yield agent == null ? null : agent.getName();
            }
            case 2 -> { // 外呼（网关）
                FsSipGateway gateway = iFsSipGatewayService.getById(Long.valueOf(routeValue));
                yield gateway == null ? null : gateway.getName();
            }
            case 3 -> routeValue; // SIP 地址
            case 4 -> { // 技能组
                CallSkill skill = iCallSkillService.getById(Long.valueOf(routeValue));
                yield skill == null ? null : skill.getName();
            }
            case 5 -> { // 放音（语音文件）
                VoiceFileVo voiceFile = iVoiceFileService.getDetail(Long.valueOf(routeValue));
                yield voiceFile == null ? null : voiceFile.getName();
            }
            case 6 -> { // IVR 流程
                FlowInfoVo flow = iFlowInfoService.getInfo(Long.valueOf(routeValue));
                yield flow == null ? null : flow.getName();
            }
            case 7 -> routeValue; // 座机分机号
            case 8 -> null; // AI 智能坐席（单一实例，类型名已表达）
            default -> null;
        };
    }

    @Log(title = "客户ASR转写", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按callId查客户侧ASR转写", method = "GET")
    @RequestMapping(value = "/user/txt", method = RequestMethod.GET)
    public ResResult<List<DialogRecord>> userTxt(@RequestParam("callId") String callId) {
        return success(iDialogueService.userTxt(callId));
    }

    @Log(title = "AI回复转写", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按callId查AI回复", method = "GET")
    @RequestMapping(value = "/ai/txt", method = RequestMethod.GET)
    public ResResult<List<DialogRecord>> aiTxt(@RequestParam("callId") String callId) {
        return success(iDialogueService.aiTxt(callId));
    }

    @Log(title = "过程摘要列表", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按callId查过程摘要列表", method = "GET")
    @RequestMapping(value = "/abstract/txt", method = RequestMethod.GET)
    public ResResult<List<AbstractRecord>> abstractTxt(@RequestParam("callId") String callId) {
        return success(iDialogueService.abstractTxt(callId));
    }

    @Log(title = "挂断全文摘要", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "按callId查挂断全文摘要(CallAiSummary 6字段)", method = "GET")
    @RequestMapping(value = "/summary", method = RequestMethod.GET)
    public ResResult<CallAiSummary> summary(@RequestParam("callId") String callId) {
        return success(iDialogueService.summary(callId));
    }

    // ============================== 翻译 / 改写 / 替换 ==============================

    @Log(title = "整通翻译", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按callId整通翻译(逐条ASR调LLM写transTxt)", method = "GET")
    @RequestMapping(value = "/translate", method = RequestMethod.GET)
    public ResResult translate(@RequestParam("callId") String callId) {
        iDialogueService.translate(callId);
        return success();
    }

    @Log(title = "整通AI改写", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按callId整通AI改写(逐条ASR调LLM写gaixieTxt)", method = "GET")
    @RequestMapping(value = "/aigaixie", method = RequestMethod.GET)
    public ResResult aigaixie(@RequestParam("callId") String callId) {
        iDialogueService.aigaixie(callId);
        return success();
    }

    @Log(title = "显示原文/改写切换", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按callId设置原文/改写显示开关(更新DialogRecord.gaixieWhether)", method = "GET")
    @RequestMapping(value = "/showDiaglogGaixie", method = RequestMethod.GET)
    public ResResult showDiaglogGaixie(@RequestParam("callId") String callId,
                                       @RequestParam("show") Integer show) {
        iDialogueService.showDiaglogGaixie(callId, show);
        return success();
    }

    @Log(title = "全局内容替换", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按callId全局子串替换dialogTxt", method = "GET")
    @RequestMapping(value = "/replaceContent", method = RequestMethod.GET)
    public ResResult replaceContent(@RequestParam("callId") String callId,
                                    @RequestParam("oldStr") String oldStr,
                                    @RequestParam("newStr") String newStr) {
        iDialogueService.replaceContent(callId, oldStr, newStr);
        return success();
    }

    @Log(title = "单条内容子串替换", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按dialogId子串替换该条dialogTxt", method = "GET")
    @RequestMapping(value = "/replaceContentByDialogId", method = RequestMethod.GET)
    public ResResult replaceContentByDialogId(@RequestParam("dialogId") Long dialogId,
                                              @RequestParam("oldStr") String oldStr,
                                              @RequestParam("newStr") String newStr) {
        iDialogueService.replaceContentByDialogId(dialogId, oldStr, newStr);
        return success();
    }

    @Log(title = "单条内容覆盖", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按dialogId整体覆盖该条dialogTxt", method = "GET")
    @RequestMapping(value = "/replaceContentByDialogIdAll", method = RequestMethod.GET)
    public ResResult replaceContentByDialogIdAll(@RequestParam("dialogId") Long dialogId,
                                                 @RequestParam("newStr") String newStr) {
        iDialogueService.replaceContentByDialogIdAll(dialogId, newStr);
        return success();
    }

    // ============================== 编辑 / 重算 ==============================

    @Log(title = "编辑摘要字段", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "人工编辑全文总结/关键词/角色/要点/代办/思维导图", method = "PUT")
    @RequestMapping(value = "/editSummary", method = RequestMethod.PUT)
    public ResResult editSummary(@RequestBody CallAiSummaryEditQuery query) {
        iDialogueService.editSummary(query);
        return success();
    }

    @Log(title = "编辑过程摘要", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "人工编辑过程摘要(标题/内容/原文)", method = "PUT")
    @RequestMapping(value = "/editAbstract", method = RequestMethod.PUT)
    public ResResult editAbstract(@RequestBody AbstractRecordEditQuery query) {
        iDialogueService.editAbstract(query);
        return success();
    }

    @Log(title = "重新生成单字段", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "按callId+key重新生成单字段(summary/keyword/role/keypoint/todo/xmind)", method = "GET")
    @RequestMapping(value = "/refreshByKey", method = RequestMethod.GET)
    public ResResult refreshByKey(@RequestParam("callId") String callId,
                                  @RequestParam("key") String key) {
        iDialogueService.refreshByKey(callId, key);
        return success();
    }

    // ============================== 反馈 / 删除 ==============================

    @Log(title = "摘要字段反馈", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "摘要字段反馈(field=keyword/role/keypoint/todo/xmind)", method = "POST")
    @RequestMapping(value = "/feedback/summary", method = RequestMethod.POST)
    public ResResult summaryFeedback(@RequestParam("id") Long id,
                                     @RequestParam("field") String field,
                                     @RequestParam("feedback") Integer feedback) {
        iDialogueService.summaryFeedback(id, field, feedback);
        return success();
    }

    @Log(title = "过程摘要反馈", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:edit')")
    @Operation(summary = "过程摘要反馈", method = "POST")
    @RequestMapping(value = "/feedback/abstract", method = RequestMethod.POST)
    public ResResult abstractFeedback(@RequestParam("id") Long id,
                                      @RequestParam("feedback") Integer feedback,
                                      @RequestParam(value = "feedbackTxt", required = false) String feedbackTxt) {
        iDialogueService.abstractFeedback(id, feedback, feedbackTxt);
        return success();
    }

    @Log(title = "摘要反馈统计", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:dialogue:stats')")
    @Operation(summary = "摘要反馈满意率统计(6维度汇总+按天趋势)", method = "GET")
    @RequestMapping(value = "/feedback/stats", method = RequestMethod.GET)
    public ResResult<FeedbackStatsVo> feedbackStats(@RequestParam(value = "beginDate", required = false) String beginDate,
                                                    @RequestParam(value = "endDate", required = false) String endDate) {
        return success(iDialogueService.feedbackStats(beginDate, endDate));
    }

    @Log(title = "删除通话对话", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:dialogue:delete')")
    @Operation(summary = "按callId批量删除通话及其对话/摘要", method = "POST")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResResult delete(@RequestBody List<String> callIds) {
        return success(iDialogueService.deleteByCallIds(callIds));
    }
}
