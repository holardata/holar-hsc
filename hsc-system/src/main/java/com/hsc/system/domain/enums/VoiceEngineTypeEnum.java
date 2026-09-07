package com.hsc.system.domain.enums;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * 语音引擎类型枚举（unify-voice-engine-config）。
 *
 * <p>类型 = 协议适配器（代码写死、低频扩展）；实例 = voice_engine 表数据（用户自由添加）。
 * 此枚举是 Java 适配器（hsc-system.util.tts / AsrConnectivityTester）、前端参数表单 schema、
 * ai-callbot RuntimeConfig 平铺字段 三方对齐的唯一事实源，改动需两侧同步。
 *
 * <p>flatFields：该类型实例 config JSON 允许的 key（与 ai-callbot RuntimeConfig 平铺字段名一致）。
 * 展开（aiCallbotConfig/get 读时展开）= flagField 置 flagValue + 从实例 config 平铺拷贝 flatFields。
 */
public enum VoiceEngineTypeEnum {

    // ---------------- ASR ----------------
    FUNASR("funasr", "ASR本地", "asr", "asr_engine", "local",
            List.of("local_asr_url")),
    ALIYUN_NLS("aliyun-nls", "阿里云NLS", "asr", "asr_engine", "aliyun",
            List.of("aliyun_asr_url", "aliyun_asr_appkey", "aliyun_asr_token")),
    TENCENT_ASR("tencent-asr", "腾讯云实时识别", "asr", "asr_engine", "tencent",
            List.of("tencent_asr_url", "tencent_asr_app_id", "tencent_asr_secret_id", "tencent_asr_secret_key")),
    XFYUN_ASR("xfyun-asr", "讯飞RTASR", "asr", "asr_engine", "xfyun",
            List.of("xfyun_asr_url", "xfyun_asr_app_id", "xfyun_asr_api_key")),

    // ---------------- TTS ----------------
    ALIYUN_TTS("aliyun-tts", "阿里云NLS", "tts", "tts_engine", "aliyun",
            List.of("aliyun_tts_appkey", "aliyun_tts_token", "aliyun_tts_url", "aliyun_tts_voice",
                    "aliyun_tts_format", "aliyun_tts_sample_rate")),
    PRO_TTS("pro", "TTS专业版", "tts", "tts_engine", "pro",
            List.of("local_tts_pro_url", "local_tts_audio_path")),
    OPENAI_TTS("openai-tts", "OpenAI兼容", "tts", "tts_engine", "local",
            List.of("tts_local_url", "tts_local_voice", "tts_local_instructions")),
    HOLAR_TTS("holartts", "TTS本地", "tts", "tts_engine", "holartts",
            List.of("holartts_url", "holartts_voice_id")),
    VOLC_TTS("volc-tts", "火山引擎豆包", "tts", "tts_engine", "volc",
            List.of("volc_tts_url", "volc_tts_token", "volc_tts_app_id", "volc_tts_cluster",
                    "volc_tts_voice", "volc_tts_speed_ratio"));

    /** 类型标识（voice_engine.engine_type 存储值，小写连字符） */
    private final String code;
    /** 中文名（前端下拉/标签） */
    private final String label;
    /** 大类：asr / tts */
    private final String kind;
    /** RuntimeConfig 引擎标识字段名（asr_engine / tts_engine） */
    private final String flagField;
    /** 引擎标识字段的值（如 funasr 展开为 asr_engine="local"，对齐 Python 侧既有取值） */
    private final String flagValue;
    /** 该类型 config 允许的平铺字段清单 */
    private final List<String> flatFields;

    VoiceEngineTypeEnum(String code, String label, String kind, String flagField, String flagValue,
                        List<String> flatFields) {
        this.code = code;
        this.label = label;
        this.kind = kind;
        this.flagField = flagField;
        this.flagValue = flagValue;
        this.flatFields = flatFields;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getKind() {
        return kind;
    }

    public List<String> getFlatFields() {
        return flatFields;
    }

    /** 按 code 查枚举；未注册的 code 抛业务异常（防脏数据静默失败） */
    public static VoiceEngineTypeEnum of(String code) {
        for (VoiceEngineTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知语音引擎类型: " + code);
    }

    /**
     * 展开为 RuntimeConfig 平铺字段：flagField 置 flagValue + 平铺拷贝实例 config 中的 flatFields。
     * 展开结果 put 进 target（不删除 target 既有业务字段）。
     */
    public void expandTo(JSONObject instanceConfig, JSONObject target) {
        target.put(flagField, flagValue);
        if (instanceConfig != null) {
            for (String field : flatFields) {
                if (instanceConfig.containsKey(field)) {
                    target.put(field, instanceConfig.get(field));
                }
            }
        }
    }
}
