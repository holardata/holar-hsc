// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.constant;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@Data
public class FlowDataContext {

    /**
     * 呼叫地址
     */
    private String address;
    /**
     * 腿信息
     */
    private String uniqueId;
    /**
     * 呼叫ID
     */
    private Long callId;
    /**
     * 流程实例ID
     */
    private Long instanceId;
    /**
     * 流程信息
     */
    private Long flowId;

    /**
     * 通话钉死的发布版流程数据快照（start 时从 flow_info.published_flow_data 读入；
     * 之后状态机构建/节点配置读取全部基于本快照，不再回库——编辑/再发布不影响进行中通话）
     */
    private String flowJson;

    /**
     * 发布版本号（start 时读入，通话期间不变；Redis 流程缓存 key 的组成部分，
     * 保证本通话始终命中自己版本的 node/edge 缓存）
     */
    private Integer version;

    /**
     * 当前节点ID
     */
    private String currentNodeId;


    /**
     * 当前节点执行记录ID
     */
    private Long currentHistoryId;

    /**
     * 挂机原因
     */
    private String hangUpCause;

    /**
     * 语音识别引擎实例 id（voice_engine，开始节点所选；供后续语音识别类节点引用）
     */
    private Long asrEngineId;
    /**
     * 语音合成引擎实例 id（voice_engine，开始节点所选；文本放音预合成用）
     */
    private Long ttsEngineId;

    /**
     * TTS 引擎参数指纹（engineType+config 的 sha256 前 16 位，FlowStart 时算）：引擎改参自动换预合成缓存 key
     */
    private String ttsFingerprint;

    /**
     * http请求结果
     */
    private String httpResult;

    /**
     * 变量袋：节点间命名变量（收号 DTMF、HTTP 结果、赋值节点的值等）。
     * 值必须是可序列化类型（String/Number/Boolean/Map/List），随状态机上下文序列化到 Redis。
     * 初始化空 Map：兼容不含本字段的历史序列化数据（fastjson 缺字段时反序列化不会失败）。
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 取变量袋（历史数据反序列化后可能为 null，兜底空 Map）
     */
    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>();
        }
        return variables;
    }

    /**
     * 取发布版本号（升级窗口期的历史通话反序列化后可能为 null，兜底 0）
     */
    public Integer getVersion() {
        return version == null ? 0 : version;
    }
}
