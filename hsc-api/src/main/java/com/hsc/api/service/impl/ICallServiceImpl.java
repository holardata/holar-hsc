// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.hsc.api.service.ICallService;
import com.hsc.calltask.domain.entity.CallTaskAssignment;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.service.ICallTaskAssignmentService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.enums.DirectionEnum;
import com.hsc.common.enums.ProcessEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.entity.FsSipGateway;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.query.call.CallQuery;
import com.hsc.system.domain.query.call.CallRecordQuery;
import com.hsc.system.domain.query.display.CallDisplayQuery;
import com.hsc.system.domain.query.fssip.FsSipGatewayQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.call.CallRecordVo;
import com.hsc.system.domain.vo.display.CallDisplayVo;
import com.hsc.system.domain.vo.route.CallRouteVo;
import com.hsc.system.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ICallServiceImpl implements ICallService {

    /**
     * 座机代拨坐席腿(话机)振铃超时(秒)——sys_config 参数 call.desk-dial-agent-ring-timeout
     * 的代码默认值。点击代拨时坐席人在电脑旁、话机在手边，10 秒不摘机即视为不在，
     * 超时由 originate_timeout 兜底挂断整通收尾。
     */
    private static final int DESK_AGENT_RING_TIMEOUT = 10;

    @Autowired
    private FsClient fsClient;

    @Autowired
    private ISysConfigService iSysConfigService;

    @Resource
    private ISipAgentService iSipAgentService;

    @Resource
    private ICallDisplayService iCallDisplayService;

    @Autowired
    private IFsCallCacheService iFsCallCacheService;

    @Autowired
    private ICallRecordService iCallRecordService;

    @Autowired
    private IFsSipGatewayService iFsSipGatewayService;

    @Autowired
    private ISipRegService sipRegService;

    @Resource
    private ICallTaskAssignmentService iCallTaskAssignmentService;

    @Resource
    private ICustomerSeasService iCustomerSeasService;

    /**
     * 座机代拨（两段式第一段）：originate 坐席腿 user/分机（话机振铃），坐席摘机后经
     * CALL_ROUTE → executeRoute 按呼出路由出局客户腿（FsCallOutRouteHandler，第二段），
     * 客户接通由 CALL_BRIDGE 桥接两腿；挂断走既有收尾 + TaskDialFinishEvent 回写任务。
     */
    @Override
    public Long makeCall(CallQuery query) {
        if (Objects.isNull(query.getAgentId())) {
            throw new CommonException("坐席ID不能为空");
        }
        SipAgentQuery sipAgentQuery = new SipAgentQuery();
        sipAgentQuery.setId(query.getAgentId());
        List<SipAgentVo> agentList = iSipAgentService.getInfoByQuery(sipAgentQuery);

        if (CollectionUtil.isEmpty(agentList)) {
            throw new CommonException("未查询到有效坐席信息");
        }
        SipAgentVo sipAgent = agentList.get(0);
        if (StringUtils.isBlank(sipAgent.getAgentNumber())) {
            throw new CommonException("坐席未配置分机号");
        }
        // 注册兜底校验（sofia reg 实时查询）：话机未注册直接失败，不白让话机/客户振铃；
        // 前端点拨打前另有预检接口，此处兜住"预检通过后话机恰好掉线"的窗口
        if (CollectionUtil.isEmpty(sipRegService.getList(sipAgent.getAgentNumber()))) {
            throw new CommonException("话机未注册，请检查话机网络/账号后重试");
        }

        //外呼显号(与软电话直呼 outboundCall 同源同查法，客户侧外显)
        CallDisplayQuery displayQuery = new CallDisplayQuery();
        List<CallDisplayVo> displayList = iCallDisplayService.getList(displayQuery);
        if (CollectionUtil.isEmpty(displayList)) {
            throw new CommonException("未配置显号");
        }
        CallDisplayVo callDisplay = RandomUtil.randomEle(displayList);

        //前置校验呼出路由：未配路由/日程坐席摘机后也会被挂断，提前失败免得话机白响
        CallRouteVo callRoute = iFsCallCacheService.getCallRoute(query.getCallee(), 2);
        if (Objects.isNull(callRoute)) {
            throw new CommonException("未配置号码路由");
        }
        //internal 网关：坐席腿 user/分机@realm 直投已注册话机
        FsSipGatewayQuery gatewayQuery = new FsSipGatewayQuery();
        gatewayQuery.setGatewayType(0);
        List<FsSipGateway> gatewayList = iFsSipGatewayService.getList(gatewayQuery);
        if (CollectionUtil.isEmpty(gatewayList)) {
            throw new CommonException("未配置非外线网关");
        }

        Long callId = IdUtil.getSnowflakeNextId();
        String uniqueId = RandomUtil.randomNumbers(32);
        // 任务拨打身份标识（座机代拨路径）：查 assignment 校验归属后随 CallInfo 全程携带，
        // 挂断收尾据此发 TaskDialFinishEvent 回写任务联系人（软电话路径经 SIP 头携带，见 outboundCall）；
        // 手动代拨不传 assignmentId，三值为 null 零侵入
        Long assignmentId = null;
        Long taskId = null;
        Long customerId = null;
        if (Objects.nonNull(query.getAssignmentId())) {
            CallTaskAssignment assignment = iCallTaskAssignmentService.getById(query.getAssignmentId());
            if (Objects.isNull(assignment)) {
                throw new CommonException("无效的联系人ID");
            }
            if (!Objects.equals(assignment.getAgentId(), query.getAgentId())) {
                throw new CommonException("该联系人未分配给当前坐席");
            }
            assignmentId = assignment.getId();
            taskId = assignment.getTaskId();
            customerId = assignment.getCustomerId();
        }
        // 私海维度代拨：仅可拨打归属本人的私海客户（软电话路径经 X-HSC-Customer-Id 头携带，两路径同校验语义）
        if (Objects.isNull(query.getAssignmentId()) && Objects.nonNull(query.getCustomerId())) {
            CustomerSeas customer = iCustomerSeasService.getById(query.getCustomerId());
            if (Objects.isNull(customer)) {
                throw new CommonException("无效的客户ID");
            }
            if (!Objects.equals(customer.getOwnerId(), query.getAgentId())) {
                throw new CommonException("仅可拨打本人私海客户");
            }
            customerId = query.getCustomerId();
        }
        //构建呼叫总线：caller=坐席分机(坐席腿真实主叫)、callee=客户号(B腿目标，
        //坐席腿摘机后 executeRoute 按它查呼出路由出局)
        //被叫振铃超时：前端显式传值优先，未传读系统参数(参数页可调实时生效)
        Integer ringTimeout = Objects.isNull(query.getCalleeTimeOut())
                ? iSysConfigService.getInt(SysConfigKeys.RING_TIMEOUT_OUTBOUND, EslConstant.OUTBOUND_CALLEE_RING_TIMEOUT)
                : query.getCalleeTimeOut();
        CallInfo callInfo = CallInfo.builder().callId(callId)
                .agentId(sipAgent.getId()).agentNumber(sipAgent.getAgentNumber()).agentName(sipAgent.getName())
                .assignmentId(assignmentId).taskId(taskId).customerId(customerId)
                .caller(sipAgent.getAgentNumber()).callee(query.getCallee()).direction(DirectionEnum.OUTBOUND.getType())
                .callTime(DateUtil.current()).hiddenCustomer(query.getHiddenCustomer())
                .calleeTimeOut(ringTimeout)
                .callerDisplay(sipAgent.getAgentNumber()).calleeDisplay(callDisplay.getPhone())
                .build();

        callInfo.addUniqueIdList(uniqueId);
        //CALL_ROUTE：坐席腿摘机(ANSWER) → FsCallIRouteProcess 录音+回铃音+executeRoute 出局客户腿
        callInfo.setProcess(ProcessEnum.CALL_ROUTE);

        //坐席腿通道
        ChannelInfo channelInfo = ChannelInfo.builder().callId(callId).uniqueId(uniqueId).cdrType(2).type(1).directionType(1)
                .agentId(sipAgent.getId()).agentNumber(sipAgent.getAgentNumber()).agentName(sipAgent.getName())
                .callTime(DateUtil.current())
                .caller(callInfo.getCaller()).called(callInfo.getCallee()).display(callInfo.getCalleeDisplay()).build();
        callInfo.setChannelInfoMap(uniqueId, channelInfo);

        //先落缓存再 originate：坐席腿事件若先于缓存写入到达会被各 handler 静默丢弃
        iFsCallCacheService.saveCallInfo(callInfo);
        iFsCallCacheService.saveCallRel(uniqueId, callId);

        log.info("座机代拨第一段: originate坐席腿 callId:{} agent:{} agentNumber:{} callee:{}",
                callId, sipAgent.getId(), sipAgent.getAgentNumber(), query.getCallee());
        //坐席腿(话机)振铃超时读系统参数(参数页可调实时生效)
        fsClient.makeCall(callId, sipAgent.getAgentNumber(), sipAgent.getAgentNumber(), uniqueId,
                iSysConfigService.getInt(SysConfigKeys.DESK_DIAL_AGENT_RING_TIMEOUT, DESK_AGENT_RING_TIMEOUT),
                gatewayList.get(0));
        return callId;
    }

    @Override
    public CallRecordVo getCallInfo(Long callId) {
        CallRecordVo callRecordVo = new CallRecordVo();
        CallRecord callRecord = iCallRecordService.getById(callId);
        if(Objects.isNull(callRecord)){
            throw new CommonException("呼叫记录不存在");
        }
        BeanUtils.copyProperties(callRecord, callRecordVo);
        return callRecordVo;
    }

    @Override
    public List<CallRecordVo> getCallPageList(CallRecordQuery query) {
        return iCallRecordService.getCallPageList(query);
    }



}
