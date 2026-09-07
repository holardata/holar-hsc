package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.domain.query.engine.VoiceEngineAddQuery;
import com.hsc.system.domain.query.engine.VoiceEngineQuery;
import com.hsc.system.util.tts.TtsResult;

import java.util.List;

/**
 * 语音引擎实例表(VoiceEngine)表服务接口（unify-voice-engine-config）。
 * 除 CRUD 外承载「引擎解析统一入口」：所有消费方（ai-callbot 配置展开 / 语音文件合成 /
 * IVR 预合成 / 连通性测试）按 id 取实例与参数都走本服务，不得各自查表拼参。
 */
public interface IVoiceEngineService extends IBaseService<VoiceEngine> {

    void addEngine(VoiceEngineAddQuery query);

    void edit(VoiceEngineAddQuery query);

    /**
     * 删除前校验引用方（AI 坐席配置 asr/tts 引用、语音文件 tts 引用），
     * 被引用时抛 CommonException 阻止删除，避免悬空引用静默失效。
     */
    void delete(VoiceEngineQuery query);

    VoiceEngine getDetail(Long id);

    List<VoiceEngine> getPageList(VoiceEngineQuery query);

    List<VoiceEngine> getList(VoiceEngineQuery query);

    /**
     * 引擎解析统一入口：按 id 取实例（Redis 缓存，edit/delete 时失效）。
     *
     * @throws com.hsc.common.exception.CommonException id 不存在或已删除
     */
    VoiceEngine resolve(Long id);

    /** 实例参数指纹（engineType+config 的 sha256 前 16 位）：IVR 预合成缓存 key 用，引擎改参自动换 key */
    String fingerprint(VoiceEngine engine);

    /** 按引擎实例合成语音（解析 → TtsEngineAdapterFactory 分发） */
    TtsResult synthesizeByEngineId(Long engineId, String text);

    /** ASR 连通性测试（按实例类型拼参/签名后握手），返回延迟 ms */
    long testAsr(Long engineId);
}
