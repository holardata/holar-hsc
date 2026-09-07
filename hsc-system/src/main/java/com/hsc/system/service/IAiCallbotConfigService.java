package com.hsc.system.service;

import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.AiCallbotConfig;

import java.io.File;

/**
 * AI 坐席配置(ai_callbot_config)表服务接口（单例 id=1）。平迁自 reminder_backend AiCallbotConfigService。
 *
 * <p>unify-voice-engine-config：ASR/TTS 引擎参数改存 voice_engine 实例、本表 configJson 只存
 * asr_engine_id/tts_engine_id 引用；{@link #getExpandedConfig()} 读时展开为 ai-callbot
 * RuntimeConfig 平铺字段（形状与改造前一致，Python 侧零改动）。testTts/testAsr 迁至
 * IVoiceEngineService（语音引擎域统一），本接口不再承载引擎测试。
 */
public interface IAiCallbotConfigService extends IBaseService<AiCallbotConfig> {

    /** 获取单例配置(id=1)；不存在返回空壳(configJson="{}")。返回原始存储形态（含引擎引用 id，未展开）。 */
    AiCallbotConfig getSingle();

    /**
     * 获取展开后的配置 JSON（ai-callbot 拉取用）：按 asr_engine_id/tts_engine_id 查 voice_engine
     * 实例，展开为 RuntimeConfig 平铺字段。无引用 id（未配置引擎）时原样返回业务字段。
     */
    JSONObject getExpandedConfig();

    /** 保存或更新单例配置(id 固定为1)。已配引擎引用侧自动剥离前端误传的旧引擎平铺字段（防脏数据回流）。 */
    void saveOrUpdateSingle(AiCallbotConfig config);

    /** holargpt 知识库列表(调 /api/core/dataset/list，供配置页下拉)。 */
    Object listDatasets();

    /** holargpt 智能体应用列表(调 /api/core/app/list，供配置页下拉)。 */
    Object listApps();

    /**
     * TTS 合成到临时文件：按引擎实例 id 经 IVoiceEngineService 解析合成后写临时文件返回。
     * 供语音文件"语音合成"(type=2)调用。引擎不存在/合成失败抛 RuntimeException。
     */
    File synthesizeToFile(Long engineId, String text);
}
