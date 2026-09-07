package com.hsc.system.util.tts;

import com.alibaba.fastjson2.JSONObject;

/**
 * TTS 引擎适配器接口（unify-voice-engine-config）。
 *
 * <p>每种引擎类型一个实现（标注 {@code @TtsEngineType}），实现"文本进、音频出"的一次性合成。
 * 实现类逻辑平移自原 AiCallbotConfigServiceImpl 的 ttsXxx 私有方法（hutool HttpRequest，
 * 与 reminder_backend 行为一致）；调用参数 config 的 key 与该引擎类型的
 * {@code VoiceEngineTypeEnum#flatFields} 对齐（来自 voice_engine.config 或 AI 配置旧平铺字段）。
 */
public interface TtsEngineAdapter {

    /**
     * 合成语音。
     *
     * @param config 引擎连接参数（url/鉴权/音色等）
     * @param text   合成文本
     * @return 音频字节与 Content-Type
     * @throws RuntimeException 配置缺失 / 引擎返回非 2xx / 返回空音频时抛出（消息面向用户可读）
     */
    TtsResult synthesize(JSONObject config, String text);
}
