// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.CallInfoDetail;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.handler.route.FsAiRouteHandler;
import com.hsc.esl.handler.route.FsExtensionRouteHandler;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.handler.route.FlowAgentRouteHandler;
import com.hsc.ivr.handler.route.FlowCallOutRouteHandler;
import com.hsc.ivr.handler.route.FlowSipRouteHandler;
import com.hsc.ivr.handler.route.FlowSkillGroupRouteHandler;
import com.hsc.ivr.properties.FlowTransferNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 转接节点处理（routeType 1-8，与号码路由一致：1坐席/2外呼/3sip/4技能组/5放音/6转IVR/7座机/8智能坐席）。
 *
 * <p>case1-6 走 IVR 内 Flow*RouteHandler；case7/8 委托主路由 FsExtensionRouteHandler/
 * FsAiRouteHandler（与号码路由进同一 handler，行为一致，不重复实现）。
 *
 * <p>case5 放音：记 transferType=2 的 detail（IVR 流程内动作），playback 完成回调
 * 按既有机制自动流转 next；case6 转IVR：当前流程结束，notice 启动新流程实例。
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowTransferHandler")
public class FlowTransferHandler extends AbstractIFlowNodeHandler{

    private final FlowAgentRouteHandler flowAgentRouteHandler;
    private final FlowCallOutRouteHandler flowCallOutRouteHandler;
    private final FlowSipRouteHandler flowSipRouteHandler;
    private final FlowSkillGroupRouteHandler flowSkillGroupRouteHandler;
    private final FsExtensionRouteHandler fsExtensionRouteHandler;
    private final FsAiRouteHandler fsAiRouteHandler;
    private final IVoiceFileService iVoiceFileService;

    public FlowTransferHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, FlowAgentRouteHandler flowAgentRouteHandler, FlowCallOutRouteHandler flowCallOutRouteHandler, FlowSipRouteHandler flowSipRouteHandler, FlowSkillGroupRouteHandler flowSkillGroupRouteHandler, FsExtensionRouteHandler fsExtensionRouteHandler, FsAiRouteHandler fsAiRouteHandler, IVoiceFileService iVoiceFileService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.flowAgentRouteHandler = flowAgentRouteHandler;
        this.flowCallOutRouteHandler = flowCallOutRouteHandler;
        this.flowSipRouteHandler = flowSipRouteHandler;
        this.flowSkillGroupRouteHandler = flowSkillGroupRouteHandler;
        this.fsExtensionRouteHandler = fsExtensionRouteHandler;
        this.fsAiRouteHandler = fsAiRouteHandler;
        this.iVoiceFileService = iVoiceFileService;
    }


    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        FlowTransferNodeProperties transferNodeProperties = JSONObject.parseObject(flowNode.getProperties(), FlowTransferNodeProperties.class);
        if(Objects.isNull(transferNodeProperties)){
            throw new FlowNodeException("转接节点配置错误");
        }
        if(StringUtils.isBlank(transferNodeProperties.getRouteValue())){
            throw new FlowNodeException("转接节点配置错误");
        }

        switch (transferNodeProperties.getRouteType()){
            case 1 ->  flowAgentRouteHandler.handler(flowData,transferNodeProperties);
            case 2 -> flowCallOutRouteHandler.handler(flowData,transferNodeProperties);
            case 3 -> flowSipRouteHandler.handler(flowData,transferNodeProperties);
            case 4 -> flowSkillGroupRouteHandler.handler(flowData,transferNodeProperties);
            case 5 -> playTransferVoice(flowData, transferNodeProperties);
            case 6 -> transferToIvr(flowData, transferNodeProperties);
            // 座机/智能坐席委托主路由 handler（与号码路由同一实现）；转出后本流程不再流转 next，
            // 状态机与 case1-4 转坐席一样留待挂断事件链收尾
            case 7 -> fsExtensionRouteHandler.handler(flowData.getAddress(),
                    fsCallCacheService.getCallInfo(flowData.getCallId()), flowData.getUniqueId(), transferNodeProperties.getRouteValue());
            case 8 -> fsAiRouteHandler.handler(flowData.getAddress(),
                    fsCallCacheService.getCallInfo(flowData.getCallId()), flowData.getUniqueId(), transferNodeProperties.getRouteValue());
            default -> throw new FlowNodeException("转接节点配置错误");
        }
        // IVR 交互转写：转出锚点（前端渲染"转接XX"分割线，后续人工/AI 段转写在同一时间线衔接）；
        // case5 放音走 playTransferVoice 内的播报记录，不落锚点
        if (transferNodeProperties.getRouteType() != 5) {
            ivrDialogLogger.logTransfer(flowData, transferLabel(transferNodeProperties.getRouteType()));
        }
    }

    /** 转接锚点文案（业务人员视角，同 RouteTypeEnum 1-8 的语义） */
    private String transferLabel(Integer routeType) {
        return switch (routeType) {
            case 1 -> "转接软坐席";
            case 2 -> "转接外线";
            case 3 -> "转接SIP号码";
            case 4 -> "转接技能组";
            case 6 -> "转接IVR流程";
            case 7 -> "转接座机";
            case 8 -> "转接AI智能坐席";
            default -> "转接";
        };
    }

    /**
     * case5 转放音：播 routeValue 指定的语音文件，记 transferType=2（IVR 流程内动作），
     * playback 完成回调按既有机制自动流转 next。
     */
    private void playTransferVoice(FlowDataContext flowData, FlowTransferNodeProperties properties) {
        VoiceFileVo voiceFile = iVoiceFileService.getDetail(Long.valueOf(properties.getRouteValue()));
        if (Objects.isNull(voiceFile)) {
            log.error("转放音未查询到语音文件 voiceId:{} callId:{}", properties.getRouteValue(), flowData.getCallId());
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());
        if (Objects.nonNull(callInfo)) {
            CallInfoDetail detail = new CallInfoDetail();
            detail.setCallId(flowData.getCallId());
            detail.setStartTime(DateUtil.current());
            detail.setOrderNum(callInfo.getDetailList() == null ? 0 : callInfo.getDetailList().size() + 1);
            detail.setTransferType(2);
            callInfo.addDetailList(detail);
            fsCallCacheService.saveCallInfo(callInfo);
        }
        fsClient.playFile(flowData.getAddress(), flowData.getUniqueId(), voiceFile.getUuidName());
        // IVR 交互转写：播报文字=语音文件的 speech_text（无则文件名）
        ivrDialogLogger.logPlay(flowData, StringUtils.isNotBlank(voiceFile.getSpeechText())
                ? voiceFile.getSpeechText() : voiceFile.getName());
    }

    /**
     * case6 转IVR：当前流程结束，notice 启动新流程实例（新 flowId = routeValue）。
     * 用 type=3 结束事件（仅 stop 状态机+清 Redis）而非 notice(2,"end")——后者会执行
     * end 节点 handler，若结束节点配 hangUp=true 会挂断通话导致新流程起不来。
     */
    private void transferToIvr(FlowDataContext flowData, FlowTransferNodeProperties properties) {
        Long newFlowId;
        try {
            newFlowId = Long.parseLong(properties.getRouteValue());
        } catch (NumberFormatException e) {
            log.error("转IVR流程ID不合法 routeValue:{} callId:{}", properties.getRouteValue(), flowData.getCallId());
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        //先静默结束当前流程（不执行结束节点挂机），再启动新流程
        iFlowNoticeService.notice(3, "", flowData);
        iFlowNoticeService.notice(flowData.getAddress(), flowData.getCallId(), flowData.getUniqueId(), newFlowId);
    }

}
