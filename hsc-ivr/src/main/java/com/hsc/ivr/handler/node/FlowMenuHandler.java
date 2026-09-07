// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowMenuNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 菜单节点处理（businessType=3）。
 *
 * <p>放音提示后收单键 DTMF，命中 menuButtons 抛 next_按键 流转；未按键/错键重收。
 * 收号编排已下沉基类（resolvePlayFile/collectDigits），本类只保留菜单业务逻辑。
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowMenuHandler")
public class FlowMenuHandler extends AbstractIFlowNodeHandler {

    /** 菜单 DTMF 结果的通道变量名 */
    public static final String MENU_DTMF_RETURN = "MENU_DTMF_RETURN";

    private final IVoiceFileService iVoiceFileService;

    public FlowMenuHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, IVoiceFileService iVoiceFileService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.iVoiceFileService = iVoiceFileService;
    }

    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            throw new FlowNodeException("节点配置错误");
        }
        FlowMenuNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowMenuNodeProperties.class);
        if (Objects.isNull(props)) {
            throw new FlowNodeException("节点配置条件错误");
        }
        //进入节点重置重收计数（流程绕回本节点时，计数器不能是旧值）
        flowData.getVariables().remove(flowData.getCurrentNodeId() + "_retry");
        String file = resolvePlayFile(iVoiceFileService, flowData, props.getPlaybackType(), props.getFileId(),
                renderTtsContent(flowData, props.getContent()));
        collectDigits(flowData, 1, 1, props.getTimeout(),
                "#", file, resolvePlayText(iVoiceFileService, props.getPlaybackType(), props.getFileId(),
                        renderTtsContent(flowData, props.getContent())), SILENCE_PROMPT, MENU_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
    }

    @Override
    public void businessHandler(String event, FlowDataContext flowData) throws FlowNodeException {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        FlowMenuNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowMenuNodeProperties.class);
        if (Objects.isNull(props)) {
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        if (StringUtils.isEmpty(event)) {
            //未按键（超时）：达最大重试走失败出口（fail 边/结束），否则播未按键提示重收
            if (retriedOut(flowData, props.getMaxRetries())) {
                noticeFail(flowData);
                return;
            }
            String notFile = resolvePlayFile(iVoiceFileService, flowData, props.getNotPlaybackType(), props.getNotFileId(),
                    renderTtsContent(flowData, props.getNotContent()));
            collectDigits(flowData, 1, 1, props.getTimeout(),
                    "#", notFile, resolvePlayText(iVoiceFileService, props.getNotPlaybackType(), props.getNotFileId(),
                            renderTtsContent(flowData, props.getNotContent())), SILENCE_PROMPT, MENU_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
        } else {
            boolean isMatch = props.getMenuButtons().stream().anyMatch(menuButton -> Objects.equals(menuButton.getButtonValue(), event));
            if (isMatch) {
                //按键存变量袋（下游条件/混播可用）
                flowData.getVariables().put(flowData.getCurrentNodeId(), event);
                iFlowNoticeService.notice(2, "next_" + event, flowData);
            } else {
                //错键：达最大重试走失败出口（fail 边/结束），否则播错误提示重收
                if (retriedOut(flowData, props.getMaxRetries())) {
                    noticeFail(flowData);
                    return;
                }
                String errorFile = resolveErrorFile(flowData, props);
                collectDigits(flowData, 1, 1, props.getTimeout(),
                        "#", errorFile, resolvePlayText(iVoiceFileService, props.getErrorPlaybackType(), props.getErrorFileId(),
                                renderTtsContent(flowData, props.getErrorContent())), SILENCE_PROMPT, MENU_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
            }
        }
    }

    /**
     * 错误提示音（未配置兜底静音）
     */
    private String resolveErrorFile(FlowDataContext flowData, FlowMenuNodeProperties props) {
        String errorFile = resolvePlayFile(iVoiceFileService, flowData, props.getErrorPlaybackType(), props.getErrorFileId(),
                renderTtsContent(flowData, props.getErrorContent()));
        return StringUtils.isBlank(errorFile) ? "silence_stream://250" : errorFile;
    }
}
