// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.handler.node;

import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.constant.EslConstant;
import com.hsc.common.constant.FlowDataContext;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.exception.FlowNodeException;
import com.hsc.common.utils.SpelUtil;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.service.IFlowNoticeService;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.ivr.domain.vo.FlowEdgeVo;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.ivr.service.IFlowInstancesService;
import com.hsc.ivr.service.IvrDialogLogger;
import com.hsc.ivr.service.IvrTtsCacheService;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.service.IVoiceFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 抽象节点处理
 *
 * @author danmo
 * @date 2024-12-26
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractIFlowNodeHandler implements IFlowNodeHandler {

    /** 兜底静音提示（FS invalid_file 统一传此值：重试节奏全由回调控制，FS 内部补播 invalid 只会多播一遍） */
    protected static final String SILENCE_PROMPT = "silence_stream://250";

    /** FS 侧收键正则（放宽为任意键：位数/评分等合法性由后端回调判定，避免 FS regexp 拦键后走错提示分支） */
    protected static final String ANY_KEY_REGEXP = "[*0-9#]+";

    protected final RedisStateMachinePersister<Object, Object> persister;
    protected final IFsCallCacheService fsCallCacheService;
    protected final IFlowNoticeService iFlowNoticeService;
    protected final IFlowInfoService iFlowInfoService;
    protected final IFlowInstancesService iFlowInstancesService;
    protected final FsClient fsClient;
    protected final RedisService redisService;

    /**
     * IVR 文本放音预合成缓存（unify-voice-engine-config：替代 FS say:/MRCP 死链路）。
     * 字段注入而非构造参数：基类构造器签名被全部节点子类引用，避免连锁改动
     * （同 FlowStartHandler 的 @Autowired 先例）。
     */
    @Autowired
    protected IvrTtsCacheService ivrTtsCacheService;

    /**
     * IVR 交互转写落库（播报/按键/事件/转接锚点 → dialog_record source=ivr）。
     * 字段注入而非构造参数：基类构造器签名被全部节点子类引用，避免连锁改动
     * （同 ivrTtsCacheService 先例）。
     */
    @Autowired
    protected IvrDialogLogger ivrDialogLogger;


    @Override
    public void handle(StateContext<Object, Object> stateContext) {
        StateMachine<Object, Object> stateMachine = stateContext.getStateMachine();
        FlowDataContext flowData = stateContext.getExtendedState().get("flowData", FlowDataContext.class);
        try {
            execute(flowData);
        } catch (FlowNodeException e) {
            if (!stateMachine.isComplete()) {
                flowData.setHangUpCause(e.getMessage());
                iFlowNoticeService.notice(2, "end", flowData);
            }
        }
        try {
            persister.persist(stateMachine, StringUtils.format(CacheConstants.CALL_IVR_INSTANCES_KEY, flowData.getInstanceId()));
        } catch (Exception e) {
            log.error("持久化状态机异常:event:{},error:{}", stateContext.getEvent(), e.getMessage(), e);
        }
        if (stateMachine.isComplete()) {
            iFlowNoticeService.notice(3, "", flowData);
        }


    }

    public abstract void execute(FlowDataContext flowData) throws FlowNodeException;



    protected FlowNodeVo getFlowNode(FlowDataContext flowData) {
        // key 带发布版本号：通话钉死进入时的版本，编辑/再发布不影响进行中通话
        return redisService.getCacheMapValue(
                StringUtils.format(CacheConstants.CALL_IVR_FLOW_INFO_NODE_KEY, flowData.getFlowId(), flowData.getVersion()),
                flowData.getCurrentNodeId());
    }

    /**
     * 持久化 currentNodeId 到 callInfo（收号类节点 execute 启动采集后挂起等 DTMF 回调，
     * ESL 回调按 currentNodeId 路由，不持久化会取到过期值）。照搬 FlowMenuHandler 的写法。
     */
    protected void persistFlowData(FlowDataContext flowData) {
        CallInfo callInfo = fsCallCacheService.getCallInfo(flowData.getCallId());
        if (Objects.nonNull(callInfo)) {
            callInfo.setFlowDataContext(flowData);
            fsCallCacheService.saveCallInfo(callInfo);
        }
    }

    /**
     * 解析提示音文件名（文件模式取语音库 uuidName；文本模式经变量混播渲染后预合成，返回缓存文件相对路径）。
     * 放音/菜单/收号/满意度共用（原逻辑分散在各 handler 重复实现）。
     *
     * <p>unify-voice-engine-config：文本模式从 FS {@code say:} + MRCP（mod_unimrcp 未加载，链路不可用）
     * 改为后端预合成——{@link IvrTtsCacheService} 按「引擎实例+参数指纹+渲染后文本」hash 缓存，
     * 命中直接 playback；未配引擎/合成失败返回 silence 兜底（跳过放音不挂死流程）。
     *
     * @param voiceFileService 语音文件服务（子类已注入，经参数传入避免基类加构造依赖）
     * @param playbackType     1-语音文件 2-文本内容
     * @param fileId           文件模式语音文件ID
     * @param content          文本模式内容（已做 ${变量} 混播替换）
     * @return FS playback/play_and_get_digits 可用的文件串
     */
    protected String resolvePlayFile(IVoiceFileService voiceFileService, FlowDataContext flowData, Integer playbackType, Long fileId, String content) {
        if (Objects.equals(playbackType, 1)) {
            // 文件模式：未配置文件ID(画布空串)或文件已被删 → 兜底静音，与文本模式失败容错对齐，
            // 不抛 NPE 中断节点(play_and_get_digits 不下发会导致收号"无反应")
            VoiceFileVo voiceFile = fileId == null ? null : voiceFileService.getDetail(fileId);
            if (Objects.isNull(voiceFile)) {
                log.warn("放音文件未配置或不存在 flowId:{} node:{} fileId:{}，本次以静音兜底",
                        flowData.getFlowId(), flowData.getCurrentNodeId(), fileId);
                return "silence_stream://250";
            }
            return voiceFile.getUuidName();
        }
        if (Objects.equals(playbackType, 2)) {
            String rendered = renderTtsContent(flowData, content);
            String cacheFile = ivrTtsCacheService.synthToCacheFile(
                    flowData.getTtsEngineId(), flowData.getTtsFingerprint(), rendered);
            return cacheFile != null ? cacheFile : "silence_stream://250";
        }
        return "silence_stream://250";
    }

    /**
     * 播报提示的转写文字（IVR 交互转写用，与 resolvePlayFile 同参）：文本模式=渲染后内容；
     * 文件模式=语音文件的 speech_text（无则文件名）；其他返回 null（不落播报记录）。
     */
    protected String resolvePlayText(IVoiceFileService voiceFileService, Integer playbackType, Long fileId, String content) {
        if (Objects.equals(playbackType, 2)) {
            return content;
        }
        if (Objects.equals(playbackType, 1) && fileId != null) {
            VoiceFileVo voiceFile = voiceFileService.getDetail(fileId);
            if (Objects.nonNull(voiceFile)) {
                return StringUtils.isNotBlank(voiceFile.getSpeechText())
                        ? voiceFile.getSpeechText() : voiceFile.getName();
            }
        }
        return null;
    }

    /**
     * 放音文本 ${变量} 混播（playbackType=2 时用）：用变量袋替换占位符后返回。
     */
    protected String renderTtsContent(FlowDataContext flowData, String content) {
        return SpelUtil.renderTemplate(content, flowData.getVariables());
    }

    /**
     * 菜单/收号/满意度共用的 DTMF 采集发起（play_and_get_digits）。
     * 原逻辑写在 FlowMenuHandler，下沉到基类供各收号节点复用（var_name 参数化解决串台）。
     * 不收 tries 参数：固定传 FS 1（一次下发只播一轮），重试轮次由回调侧 retriedOut 统一控制，
     * 每轮之间可播不同提示（首轮/未按键/错键），避免 FS 内层重播与回调外层计数相乘。
     *
     * @param regexp     按键正则（菜单单键 "[*0-9#]+"；收号按 numMin/numMax 定）
     * @param promptText 本轮播报提示的转写文字（IVR 交互转写落库；每轮真正下发播放时落，
     *                   含重试轮的错误/未按键提示重播，null 跳过）
     */
    protected void collectDigits(FlowDataContext flowData, Integer min, Integer max, Integer timeout,
                                 String terminator, String file, String promptText, String errorFile, String varName, String regexp,
                                 Integer digitTimeout) {
        ivrDialogLogger.logPlay(flowData, promptText);
        // 下发前回存 flowData 到 callInfo：businessHandler 重收分支修改的变量袋(_retry 计数等)
        // 必须落回 Redis，否则下次回调取到旧对象、计数永远归零 → maxRetries 失效无限重播
        persistFlowData(flowData);
        // FS play_and_get_digits 参数兜底（画布未配置时传 0 会破坏 FS 行为）：
        // - max≤0 → 128(FS 上限=不限位)：FS"收满 max 位即返回"对 0 恒真，会播完不等按键立即
        //   返回空且不收键（timeout 配多大都不生效、按键全丢），位数合法性由后端 isCompliant 校验；
        // - timeout≤0 → 5000ms：播完等待按键的时长，过短会导致立即重播。
        int fsMin = min == null || min < 0 ? 0 : min;
        int fsMax = max == null || max <= 0 ? 128 : max;
        int fsTimeout = timeout == null || timeout <= 0 ? 5000 : timeout;
        int fsDigitTimeout = digitTimeout == null || digitTimeout <= 0 ? fsTimeout : digitTimeout;
        fsClient.playAndGetDigits(flowData.getAddress(), flowData.getUniqueId(), fsMin, fsMax, 1, fsTimeout,
                terminator, file, errorFile, varName, regexp, fsDigitTimeout, null);
    }

    /**
     * 重收限次（菜单/收号共用）：变量袋计已重收次数（键=节点id_retry），超出 maxRetries 返回 true。
     * 每次未按键/错键回调时先调用本方法判定，未超限才重发采集。
     */
    protected boolean retriedOut(FlowDataContext flowData, Integer maxRetries) {
        String retryKey = flowData.getCurrentNodeId() + "_retry";
        int retried = Integer.parseInt(String.valueOf(flowData.getVariables().getOrDefault(retryKey, "0")));
        int max = maxRetries == null ? 1 : maxRetries;
        if (retried >= max) {
            return true;
        }
        flowData.getVariables().put(retryKey, String.valueOf(retried + 1));
        return false;
    }

    /**
     * 重试耗尽的出口：节点配了 event="fail" 的出边（画布"重试耗尽跳转"生成的边）则走 fail 流转，
     * 未配置维持 end 结束流程（兼容存量流程）。
     */
    protected void noticeFail(FlowDataContext flowData) {
        ivrDialogLogger.logNote(flowData, "重试耗尽");
        Map<String, List<FlowEdgeVo>> edgeMap = redisService.getCacheMap(
                StringUtils.format(CacheConstants.CALL_IVR_FLOW_INFO_EDGE_KEY, flowData.getFlowId(), flowData.getVersion()));
        boolean hasFail = edgeMap != null && edgeMap
                .getOrDefault(flowData.getCurrentNodeId(), List.of()).stream()
                .anyMatch(edge -> "fail".equals(edge.getEvent()));
        iFlowNoticeService.notice(2, hasFail ? "fail" : "end", flowData);
    }

    /**
     * 放音打断设置（仅放音节点使用）：true=任意键打断（playback_terminators=any，按键即跳过播报且键被消费），
     * false=听完（none）。
     * ⚠️ 只对纯 playback 生效——play_and_get_digits（菜单/收号/满意度）走 play_file 的 args.buf 路径，
     * FS 不读此变量，收键期间按有效键天然即收即停，无需也不受此设置影响。
     */
    protected void applyInterrupt(FlowDataContext flowData, Boolean interrupt) {
        if (Boolean.TRUE.equals(interrupt)) {
            fsClient.sendArgs(flowData.getAddress(), flowData.getUniqueId(), EslConstant.SET, EslConstant.PLAYBACK_TERMINATORS_ANY);
        } else {
            fsClient.sendArgs(flowData.getAddress(), flowData.getUniqueId(), EslConstant.SET, EslConstant.PLAYBACK_TERMINATORS);
        }
    }
}
