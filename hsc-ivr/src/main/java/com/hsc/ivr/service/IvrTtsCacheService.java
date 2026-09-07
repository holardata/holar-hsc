package com.hsc.ivr.service;

/**
 * IVR 文本放音预合成缓存（unify-voice-engine-config）。
 *
 * <p>替代原 FS {@code say:} + MRCP 死链路（mod_unimrcp 未加载，链路不可用）：文本先经变量混播渲染，
 * 按「引擎实例 id + 引擎参数指纹 + 渲染后文本」hash 缓存音频文件，命中直接 playback、
 * 未命中经统一 TTS 合成入口落盘后播放。文件落在 /temp/voice/tts-cache/（与 FS sounds 共享挂载）。
 */
public interface IvrTtsCacheService {

    /**
     * 预合成并返回 FS playback 相对路径（如 tts-cache/abc123.wav，相对 sounds 目录）。
     *
     * @param engineId    TTS 引擎实例 id（来自流程上下文，开始节点所选）
     * @param fingerprint 引擎参数指纹（FlowStart 时计算，引擎改参自动换缓存 key）
     * @param renderedText 已完成 ${变量} 混播渲染的文本
     * @return playback 相对路径；引擎未配置或合成失败返回 null（调用方以 silence 兜底，不挂死流程）
     */
    String synthToCacheFile(Long engineId, String fingerprint, String renderedText);
}
