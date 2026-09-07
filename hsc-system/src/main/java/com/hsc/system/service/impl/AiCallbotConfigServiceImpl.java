package com.hsc.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.AiCallbotConfig;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.domain.enums.VoiceEngineTypeEnum;
import com.hsc.system.mapper.AiCallbotConfigMapper;
import com.hsc.system.service.IAiCallbotConfigService;
import com.hsc.system.service.IVoiceEngineService;
import com.hsc.system.util.HolarGptListClient;
import com.hsc.system.util.tts.TtsResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * AI 坐席配置(ai_callbot_config)表服务实现（单例 id=1）。平迁自 reminder_backend AiCallbotConfigServiceImpl。
 *
 * <p>listDatasets/listApps 调 holargpt（HTTP/解析收口于 {@link HolarGptListClient}，与 HumanAgentConfigServiceImpl 共用）。
 *
 * <p>unify-voice-engine-config：引擎参数外迁 voice_engine 表后，本类收敛为「配置存储 + 读时展开 +
 * 合成薄壳」。四引擎合成实现平移至 tts 适配器（@TtsEngineType 注解驱动 Factory），
 * testTts/testAsr 迁至 {@link VoiceEngineServiceImpl}（语音引擎域统一）。
 * 注：rb 有 Redis 缓存，hsc 暂用直查（对齐 HumanAgentConfig 范式）；ai-callbot 高频拉取走 getSingle 缓存。
 */
@Slf4j
@Service
public class AiCallbotConfigServiceImpl
        extends BaseServiceImpl<AiCallbotConfigMapper, AiCallbotConfig>
        implements IAiCallbotConfigService {

    private static final long SINGLE_ID = 1L;

    /** Redis 缓存 key（存整个配置实体 JSON）。ai-callbot 每通电话拉取，高频，走缓存。
     *  带 TTL：兜底直改 DB 等旁路变更场景，保证最多 60s 后读到新形态。 */
    private static final String CACHE_KEY = "ai_callbot:config";
    private static final long CACHE_TTL_SECONDS = 60;

    @Resource
    private IVoiceEngineService voiceEngineService;

    @Resource
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Override
    public AiCallbotConfig getSingle() {
        // 1. 先读 Redis 缓存
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached != null) {
                AiCallbotConfig cfg = JSON.parseObject(cached.toString(), AiCallbotConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            }
        } catch (Exception e) {
            log.warn("读取 AI 坐席配置 Redis 缓存失败，回源 DB", e);
        }
        // 2. 未命中，回源 MySQL（单例 id=1）
        AiCallbotConfig cfg = getById(SINGLE_ID);
        if (cfg == null) {
            cfg = new AiCallbotConfig();
            cfg.setId(SINGLE_ID);
            cfg.setConfigJson("{}");
        }
        // 3. 回填 Redis（带 TTL：兜底直改 DB 等旁路变更，防陈旧形态长期生效）
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, JSON.toJSONString(cfg), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("回填 AI 坐席配置 Redis 缓存失败", e);
        }
        return cfg;
    }

    @Override
    public JSONObject getExpandedConfig() {
        AiCallbotConfig cfg = getSingle();
        JSONObject config = StrUtil.isBlank(cfg.getConfigJson())
                ? new JSONObject() : JSON.parseObject(cfg.getConfigJson());
        // 读时展开（存引用）：asr/tts 引用 id → voice_engine 实例 → RuntimeConfig 平铺字段。
        // 无引用 id（AI 配置未选引擎）时原样返回业务字段，引擎参数段由 ai-callbot 侧 INITIAL_DEFAULTS 兜底。
        expandEngineRef(config, "asr_engine_id", "asr");
        expandEngineRef(config, "tts_engine_id", "tts");
        return config;
    }

    /** 展开单个引擎引用；id 为空跳过（未配置引擎）；id 非空但实例不存在/类型不符抛错（引用明确报错，不静默降级） */
    private void expandEngineRef(JSONObject config, String refField, String expectKind) {
        Long engineId = config.getLong(refField);
        if (engineId == null) {
            return;
        }
        VoiceEngine engine = voiceEngineService.resolve(engineId);
        VoiceEngineTypeEnum type = VoiceEngineTypeEnum.of(engine.getEngineType());
        if (!Objects.equals(type.getKind(), expectKind)) {
            throw new CommonException(String.format("引擎引用类型不符：引用实例「%s」是 %s 类型，期望 %s",
                    engine.getName(), type.getLabel(), expectKind.toUpperCase()));
        }
        JSONObject instanceConfig = StrUtil.isBlank(engine.getConfig()) ? new JSONObject()
                : JSON.parseObject(engine.getConfig());
        type.expandTo(instanceConfig, config);
    }

    @Override
    public void saveOrUpdateSingle(AiCallbotConfig config) {
        // merge：以旧 configJson 为基础，新提交字段覆盖。
        // 前端 onSave 过滤空字段（只提交非空），merge 后空字段不覆盖旧预置（transfer_prompt 等话术不被清空）
        AiCallbotConfig existing = getById(SINGLE_ID);
        JSONObject merged = (existing != null && StrUtil.isNotBlank(existing.getConfigJson()))
                ? JSON.parseObject(existing.getConfigJson()) : new JSONObject();
        if (StrUtil.isNotBlank(config.getConfigJson())) {
            JSONObject incoming = JSON.parseObject(config.getConfigJson());
            for (String k : incoming.keySet()) {
                merged.put(k, incoming.get(k));
            }
            // 引擎引用解除：前端清空引擎下拉时提交 asr/tts_engine_id=null（fastjson2 默认序列化会丢 null 值），
            // 此处显式 remove key 才能真正解除引用（否则被引用的引擎实例永远删不掉）
            removeEngineRefIfCleared(incoming, merged, "asr_engine_id");
            removeEngineRefIfCleared(incoming, merged, "tts_engine_id");
        }
        // 已配引用侧剥离旧引擎平铺字段，防前端误传脏数据回流。
        // 按侧剥离（asr/tts 各自判断）：只配了一侧引擎时，另一侧历史残留字段不受影响。
        if (merged.containsKey("asr_engine_id")) {
            stripLegacyEngineFields(merged, "asr");
        }
        if (merged.containsKey("tts_engine_id")) {
            stripLegacyEngineFields(merged, "tts");
        }
        AiCallbotConfig toSave = existing != null ? existing : new AiCallbotConfig();
        toSave.setId(SINGLE_ID);
        toSave.setConfigJson(merged.toJSONString());
        saveOrUpdate(toSave);
        // 同步刷新缓存（save 即刷，下一通电话生效）
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, JSON.toJSONString(toSave), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("刷新 AI 坐席配置 Redis 缓存失败", e);
        }
    }

    /** 移除指定大类（asr/tts）的旧引擎平铺字段（引擎标识 + 该大类各类型 flatFields，参数已迁 voice_engine 表） */
    private void stripLegacyEngineFields(JSONObject merged, String kind) {
        merged.remove("asr".equals(kind) ? "asr_engine" : "tts_engine");
        for (VoiceEngineTypeEnum type : VoiceEngineTypeEnum.values()) {
            if (type.getKind().equals(kind)) {
                for (String field : type.getFlatFields()) {
                    merged.remove(field);
                }
            }
        }
    }

    /** incoming 显式携带引用字段且值为 null（前端清空下拉）→ 从 merged 移除该 key（解除引用） */
    private void removeEngineRefIfCleared(JSONObject incoming, JSONObject merged, String refField) {
        if (incoming.containsKey(refField) && incoming.get(refField) == null) {
            merged.remove(refField);
        }
    }

    @Override
    public Object listDatasets() {
        return fetchHolarGptList("/api/core/dataset/list", "知识库列表");
    }

    @Override
    public Object listApps() {
        return fetchHolarGptList("/api/core/app/list", "智能体列表");
    }

    /**
     * TTS 合成到临时文件：按引擎实例 id 经解析统一入口取参数合成，写临时文件返回。
     * 供语音文件"语音合成"(type=2)调用；引擎不存在/合成失败抛 RuntimeException（msg 面向用户）。
     */
    @Override
    public File synthesizeToFile(Long engineId, String text) {
        if (StrUtil.isBlank(text)) {
            throw new RuntimeException("合成文本不能为空");
        }
        TtsResult result = voiceEngineService.synthesizeByEngineId(engineId, text);
        String suffix = "audio/mpeg".equalsIgnoreCase(result.getContentType()) ? "mp3" : "wav";
        File file = FileUtil.file(FileUtil.getTmpDir(),
                "tts_" + IdUtil.fastSimpleUUID() + "." + suffix);
        FileUtil.writeBytes(result.getBody(), file);
        return file;
    }

    // ---------------- 内部：HolarGPT 列表 ----------------

    private Object fetchHolarGptList(String endpoint, String label) {
        AiCallbotConfig cfg = getSingle();
        JSONObject config = StrUtil.isBlank(cfg.getConfigJson())
                ? new JSONObject() : JSON.parseObject(cfg.getConfigJson());
        return HolarGptListClient.list(
                config.getString("holargpt_base_url"),
                config.getString("holargpt_list_api_key"),
                endpoint, label);
    }
}
