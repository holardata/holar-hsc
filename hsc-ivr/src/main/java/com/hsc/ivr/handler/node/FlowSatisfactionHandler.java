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
import com.hsc.ivr.domain.entity.IvrSatisfactionRecord;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.properties.FlowReceiveNodeProperties;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.ivr.service.IIvrSatisfactionRecordService;
import com.hsc.system.service.IVoiceFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 满意度节点处理（businessType=7）。
 *
 * <p>播放评分提示、收单键评分（1-5），落库 ivr_satisfaction_record 后流转下一节点。
 * 评分是可选交互：超时/无效按键落 dtmf=null、score=0 继续流程，不挂机不卡死。
 * var_name 用 SATISFACTION_DTMF_RETURN（独立于菜单/收号，防串台）。
 * 节点配置复用 FlowReceiveNodeProperties（提示音/超时/重试字段同构）。
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@Component("FlowSatisfactionHandler")
public class FlowSatisfactionHandler extends AbstractIFlowNodeHandler {

    /** 满意度 DTMF 结果的通道变量名（独立 var_name，防与菜单/收号串台） */
    public static final String SATISFACTION_DTMF_RETURN = "SATISFACTION_DTMF_RETURN";

    /** 评分正则：1-5 单键 */
    private static final String SCORE_REGEXP = "[1-5]";

    private final IVoiceFileService iVoiceFileService;
    private final IIvrSatisfactionRecordService satisfactionRecordService;

    public FlowSatisfactionHandler(RedisStateMachinePersister<Object, Object> persister, IFsCallCacheService fsCallCacheService, IFlowNoticeService iFlowNoticeService, IFlowInfoService iFlowInfoService, IFlowInstancesService iFlowInstancesService, FsClient fsClient, RedisService redisService, IVoiceFileService iVoiceFileService, IIvrSatisfactionRecordService satisfactionRecordService) {
        super(persister, fsCallCacheService, iFlowNoticeService, iFlowInfoService, iFlowInstancesService, fsClient, redisService);
        this.iVoiceFileService = iVoiceFileService;
        this.satisfactionRecordService = satisfactionRecordService;
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
        //进入节点重置重收计数
        flowData.getVariables().remove(flowData.getCurrentNodeId() + "_retry");
        String file = resolvePlayFile(iVoiceFileService, flowData, props.getPlaybackType(), props.getFileId(),
                renderTtsContent(flowData, props.getContent()));
        collectDigits(flowData, 1, 1, props.getTimeout(),
                "#", file, resolvePlayText(iVoiceFileService, props.getPlaybackType(), props.getFileId(),
                        renderTtsContent(flowData, props.getContent())), SILENCE_PROMPT, SATISFACTION_DTMF_RETURN, ANY_KEY_REGEXP, props.getTimeout());
    }

    @Override
    public void businessHandler(String event, FlowDataContext flowData) throws FlowNodeException {
        FlowNodeVo flowNode = getFlowNode(flowData);
        FlowReceiveNodeProperties props = flowNode == null ? null
                : JSONObject.parseObject(flowNode.getProperties(), FlowReceiveNodeProperties.class);
        if (StringUtils.isBlank(event)) {
            //未按键（超时）：重播提示重收（与菜单/收号同语义，计数用 retriedOut）；
            //达最大重试才落"未评价"继续流程（可选交互不卡死）
            if (props == null || retriedOut(flowData, props.getMaxRetries())) {
                saveRecord(flowData, 0, null);
                iFlowNoticeService.notice(2, "next", flowData);
                return;
            }
            String file = props == null ? SILENCE_PROMPT
                    : resolvePlayFile(iVoiceFileService, flowData, props.getPlaybackType(), props.getFileId(),
                    renderTtsContent(flowData, props.getContent()));
            collectDigits(flowData, 1, 1, props == null ? 5000 : props.getTimeout(),
                    "#", file, props == null ? null
                            : resolvePlayText(iVoiceFileService, props.getPlaybackType(), props.getFileId(),
                            renderTtsContent(flowData, props.getContent())), SILENCE_PROMPT, SATISFACTION_DTMF_RETURN, ANY_KEY_REGEXP, props == null ? 5000 : props.getTimeout());
            return;
        }
        if (event.matches(SCORE_REGEXP)) {
            //有效评分：落库 + 存变量袋（键=varName 缺省节点 id，与收号节点一致，供下游条件/混播引用），
            //流转下一节点——原实现漏了 notice 导致有效评分后流程停死（静音不挂断）
            saveRecord(flowData, Integer.parseInt(event), event);
            String varKey = props == null || StringUtils.isBlank(props.getVarName())
                    ? flowData.getCurrentNodeId() : props.getVarName();
            flowData.getVariables().put(varKey, event);
            iFlowNoticeService.notice(2, "next", flowData);
        } else {
            //无效按键（非 1-5）：重收，但用变量袋计数限次（FS 侧 tries 耗尽后回调仍无效键时，
            //不无限重发，超出 maxRetries 落未评价并继续，防"持续按 0/6 按键"死循环）
            String retryKey = flowData.getCurrentNodeId() + "_retry";
            int retried = Integer.parseInt(String.valueOf(flowData.getVariables().getOrDefault(retryKey, "0")));
            int maxRetries = props == null || props.getMaxRetries() == null ? 1 : props.getMaxRetries();
            if (retried >= maxRetries) {
                saveRecord(flowData, 0, event);
                iFlowNoticeService.notice(2, "next", flowData);
                return;
            }
            flowData.getVariables().put(retryKey, String.valueOf(retried + 1));
            String file = props == null ? "silence_stream://250"
                    : resolvePlayFile(iVoiceFileService, flowData, props.getPlaybackType(), props.getFileId(),
                    renderTtsContent(flowData, props.getContent()));
            collectDigits(flowData, 1, 1, props == null ? 5000 : props.getTimeout(),
                    "#", file, props == null ? null
                            : resolvePlayText(iVoiceFileService, props.getPlaybackType(), props.getFileId(),
                            renderTtsContent(flowData, props.getContent())), SILENCE_PROMPT, SATISFACTION_DTMF_RETURN, ANY_KEY_REGEXP, props == null ? 5000 : props.getTimeout());
        }
    }

    /**
     * 落库评分记录；未按键耗尽（dtmf=null, score=0）时同步落一条【未评价】转写
     */
    private void saveRecord(FlowDataContext flowData, int score, String dtmf) {
        if (score == 0 && dtmf == null) {
            ivrDialogLogger.logNote(flowData, "未评价");
        }
        try {
            IvrSatisfactionRecord record = new IvrSatisfactionRecord();
            record.setInstanceId(flowData.getInstanceId());
            record.setCallId(flowData.getCallId());
            record.setFlowId(flowData.getFlowId());
            record.setNodeId(flowData.getCurrentNodeId());
            record.setScore(score);
            record.setDtmf(dtmf);
            satisfactionRecordService.save(record);
        } catch (Exception e) {
            //落库失败不阻断流程
            log.error("满意度评分落库失败 flowData:{}, score:{}, error:{}", flowData, score, e.getMessage(), e);
        }
    }
}
