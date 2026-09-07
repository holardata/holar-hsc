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
import com.hsc.ivr.properties.FlowReceiveNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 收号节点处理（businessType=4）。
 *
 * <p>采集 DTMF 并按 numMax/numMin/endPoint/timeout/maxRetries 校验：
 * 合规按键写入变量袋（键=varName，缺省节点 id）后流转下一节点；
 * 不合规播错误提示重收；达最大重试/超时走 end 结束流程。
 * var_name 用 RECEIVE_DTMF_RETURN（独立于菜单，防串台）。
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowReceiveHandler")
public class FlowReceiveHandler extends AbstractIFlowNodeHandler {

    /** 收号 DTMF 结果的通道变量名（独立 var_name，防与菜单/满意度串台） */
    public static final String RECEIVE_DTMF_RETURN = "RECEIVE_DTMF_RETURN";

    private final IVoiceFileService iVoiceFileService;

    public FlowReceiveHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, IVoiceFileService iVoiceFileService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.iVoiceFileService = iVoiceFileService;
    }

    @Override
    public void execute(FlowDataContext flowData) {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            throw new FlowNodeException("节点配置错误");
        }
        FlowReceiveNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowReceiveNodeProperties.class);
        if (Objects.isNull(props)) {
            throw new FlowNodeException("节点配置条件错误");
        }
        //进入节点重置重收计数（流程经条件判断绕回本节点时，计数器不能是旧值）
        flowData.getVariables().remove(flowData.getCurrentNodeId() + "_retry");
        String file = resolvePlayFile(iVoiceFileService, flowData, props.getPlaybackType(), props.getFileId(),
                renderTtsContent(flowData, props.getContent()));
        collectDigits(flowData, numMin(props), numMax(props), props.getTimeout(),
                terminator(props), file, resolvePlayText(iVoiceFileService, props.getPlaybackType(), props.getFileId(),
                        renderTtsContent(flowData, props.getContent())), SILENCE_PROMPT, RECEIVE_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
    }

    @Override
    public void businessHandler(String event, FlowDataContext flowData) throws FlowNodeException {
        FlowNodeVo flowNode = getFlowNode(flowData);
        if (Objects.isNull(flowNode)) {
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        FlowReceiveNodeProperties props = JSONObject.parseObject(flowNode.getProperties(), FlowReceiveNodeProperties.class);
        if (Objects.isNull(props)) {
            iFlowNoticeService.notice(2, "end", flowData);
            return;
        }
        if (StringUtils.isBlank(event)) {
            //未按键（超时）：达最大重试走失败出口（fail 边/结束），否则播未按键提示重收
            if (retriedOut(flowData, props.getMaxRetries())) {
                noticeFail(flowData);
                return;
            }
            String notFile = resolvePlayFile(iVoiceFileService, flowData, props.getNotPlaybackType(), props.getNotFileId(),
                    renderTtsContent(flowData, props.getNotContent()));
            collectDigits(flowData, numMin(props), numMax(props), props.getTimeout(),
                    terminator(props), notFile, resolvePlayText(iVoiceFileService, props.getNotPlaybackType(), props.getNotFileId(),
                            renderTtsContent(flowData, props.getNotContent())), SILENCE_PROMPT, RECEIVE_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
            return;
        }
        if (isCompliant(event, props)) {
            //合规：写入变量袋（键=varName 缺省节点 id），流转下一节点
            String varKey = StringUtils.isNotBlank(props.getVarName()) ? props.getVarName() : flowData.getCurrentNodeId();
            flowData.getVariables().put(varKey, event);
            iFlowNoticeService.notice(2, "next", flowData);
        } else {
            //不合规：达最大重试走失败出口（fail 边/结束），否则播错误提示重收
            if (retriedOut(flowData, props.getMaxRetries())) {
                noticeFail(flowData);
                return;
            }
            String errorFile = resolveErrorFile(flowData, props);
            collectDigits(flowData, numMin(props), numMax(props), props.getTimeout(),
                    terminator(props), errorFile, resolvePlayText(iVoiceFileService, props.getErrorPlaybackType(), props.getErrorFileId(),
                            renderTtsContent(flowData, props.getErrorContent())), SILENCE_PROMPT, RECEIVE_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
        }
    }

    /**
     * 错误提示音（未配置兜底静音）
     */
    private String resolveErrorFile(FlowDataContext flowData, FlowReceiveNodeProperties props) {
        String errorFile = resolvePlayFile(iVoiceFileService, flowData, props.getErrorPlaybackType(), props.getErrorFileId(),
                renderTtsContent(flowData, props.getErrorContent()));
        return StringUtils.isBlank(errorFile) ? "silence_stream://250" : errorFile;
    }


    private int numMin(FlowReceiveNodeProperties props) {
        return props.getNumMin() == null ? 0 : props.getNumMin();
    }

    private int numMax(FlowReceiveNodeProperties props) {
        return props.getNumMax() == null ? 0 : props.getNumMax();
    }

    /**
     * 结束按键作 terminator（未配置默认 #）
     */
    private String terminator(FlowReceiveNodeProperties props) {
        return StringUtils.isBlank(props.getEndPoint()) ? "#" : props.getEndPoint();
    }

    /**
     * 按键位数合规校验（numMin~numMax 区间；两端未配置则不限）
     */
    private boolean isCompliant(String event, FlowReceiveNodeProperties props) {
        int len = event.length();
        int min = numMin(props);
        int max = numMax(props);
        if (min > 0 && len < min) {
            return false;
        }
        return max <= 0 || len <= max;
    }
}
