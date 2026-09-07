package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.DigestUtils;
import com.hsc.system.domain.entity.AiCallbotConfig;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.domain.entity.VoiceFile;
import com.hsc.system.domain.enums.VoiceEngineTypeEnum;
import com.hsc.system.domain.query.engine.VoiceEngineAddQuery;
import com.hsc.system.domain.query.engine.VoiceEngineQuery;
import com.hsc.system.mapper.AiCallbotConfigMapper;
import com.hsc.system.mapper.VoiceEngineMapper;
import com.hsc.system.mapper.VoiceFileMapper;
import com.hsc.system.service.IVoiceEngineService;
import com.hsc.system.util.aicallbot.AsrConnectivityTester;
import com.hsc.system.util.tts.TtsEngineAdapterFactory;
import com.hsc.system.util.tts.TtsResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 语音引擎实例表(VoiceEngine)表服务实现（unify-voice-engine-config）。
 *
 * <p>承载「引擎解析统一入口」：{@link #resolve(Long)}（Redis 缓存，写操作失效）+
 * {@link #synthesizeByEngineId(Long, String)}（经 {@link TtsEngineAdapterFactory} 按类型分发）。
 * 所有消费方（aiCallbotConfig/get 展开、语音文件合成、IVR 预合成、连通性测试）经此取参数。
 */
@Slf4j
@Service
public class VoiceEngineServiceImpl extends BaseServiceImpl<VoiceEngineMapper, VoiceEngine>
        implements IVoiceEngineService {

    /** 实例级 Redis 缓存 key 前缀（key=voice_engine:{id}，存实体 JSON） */
    private static final String CACHE_KEY_PREFIX = "voice_engine:";

    @Resource
    private AsrConnectivityTester asrConnectivityTester;

    @Resource
    private TtsEngineAdapterFactory ttsEngineAdapterFactory;

    @Resource
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Resource
    private AiCallbotConfigMapper aiCallbotConfigMapper;

    @Resource
    private VoiceFileMapper voiceFileMapper;

    @Override
    public void addEngine(VoiceEngineAddQuery query) {
        checkParams(query);
        VoiceEngine engine = new VoiceEngine();
        engine.setName(query.getName());
        engine.setEngineType(query.getEngineType());
        engine.setConfig(filterConfig(query.getEngineType(), query.getConfig()));
        engine.setRemark(query.getRemark());
        save(engine);
    }

    @Override
    public void edit(VoiceEngineAddQuery query) {
        VoiceEngine engine = getById(query.getId());
        if (Objects.isNull(engine)) {
            throw new CommonException("无效ID");
        }
        checkParams(query);
        engine.setName(query.getName());
        engine.setEngineType(query.getEngineType());
        engine.setConfig(filterConfig(query.getEngineType(), query.getConfig()));
        engine.setRemark(query.getRemark());
        updateById(engine);
        evictCache(query.getId());
    }

    @Override
    public void delete(VoiceEngineQuery query) {
        List<Long> ids = new LinkedList<>();
        if (Objects.nonNull(query.getId())) {
            ids.add(query.getId());
        }
        if (CollectionUtil.isNotEmpty(query.getIds())) {
            ids.addAll(query.getIds());
        }
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        checkReferences(ids);
        List<VoiceEngine> list = ids.stream().map(id -> {
            VoiceEngine engine = new VoiceEngine();
            engine.setId(id);
            engine.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return engine;
        }).toList();
        updateBatchById(list);
        ids.forEach(this::evictCache);
    }

    /** 删除前校验引用方：AI 坐席配置 asr/tts 引用 + 语音文件 tts 引用（IVR 流程 JSON 引用在发布时校验） */
    private void checkReferences(List<Long> ids) {
        AiCallbotConfig cfg = aiCallbotConfigMapper.selectById(1L);
        JSONObject config = (cfg != null && StrUtil.isNotBlank(cfg.getConfigJson()))
                ? JSON.parseObject(cfg.getConfigJson()) : new JSONObject();
        Long asrId = config.getLong("asr_engine_id");
        Long ttsId = config.getLong("tts_engine_id");
        for (Long id : ids) {
            if (Objects.equals(id, asrId)) {
                throw new CommonException("该引擎正在被「AI智能坐席配置」引用（ASR），请先解除引用");
            }
            if (Objects.equals(id, ttsId)) {
                throw new CommonException("该引擎正在被「AI智能坐席配置」引用（TTS），请先解除引用");
            }
        }
        Long fileCount = voiceFileMapper.selectCount(new LambdaQueryWrapper<VoiceFile>()
                .in(VoiceFile::getTts, ids.stream().map(String::valueOf).toList())
                .eq(VoiceFile::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (fileCount != null && fileCount > 0) {
            throw new CommonException("该引擎正在被「语音文件」引用（" + fileCount + " 条合成记录），请先调整后再删除");
        }
    }

    @Override
    public VoiceEngine getDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<VoiceEngine> getPageList(VoiceEngineQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return getList(query);
    }

    @Override
    public List<VoiceEngine> getList(VoiceEngineQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public VoiceEngine resolve(Long id) {
        if (Objects.isNull(id)) {
            throw new CommonException("引擎实例ID不能为空");
        }
        String cacheKey = CACHE_KEY_PREFIX + id;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                VoiceEngine engine = JSON.parseObject(cached.toString(), VoiceEngine.class);
                if (engine != null) {
                    return engine;
                }
            }
        } catch (Exception e) {
            log.warn("读取语音引擎缓存失败 id={}，回源 DB", id, e);
        }
        VoiceEngine engine = getById(id);
        if (Objects.isNull(engine) || Objects.equals(engine.getDelFlag(), DeleteStatusEnum.DELETE_YES.getIndex())) {
            throw new CommonException("语音引擎实例不存在(id=" + id + ")，请检查引用配置");
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(engine), 60, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("回填语音引擎缓存失败 id={}", id, e);
        }
        return engine;
    }

    @Override
    public String fingerprint(VoiceEngine engine) {
        return DigestUtils.sha256Hex(engine.getEngineType() + ":" + StrUtil.nullToEmpty(engine.getConfig()))
                .substring(0, 16);
    }

    @Override
    public TtsResult synthesizeByEngineId(Long engineId, String text) {
        VoiceEngine engine = resolve(engineId);
        VoiceEngineTypeEnum type = VoiceEngineTypeEnum.of(engine.getEngineType());
        if (!"tts".equals(type.getKind())) {
            throw new CommonException("引擎实例不是 TTS 类型: " + engine.getEngineType());
        }
        JSONObject config = StrUtil.isBlank(engine.getConfig()) ? new JSONObject()
                : JSON.parseObject(engine.getConfig());
        return ttsEngineAdapterFactory.synthesize(engine.getEngineType(), config, text);
    }

    @Override
    public long testAsr(Long engineId) {
        VoiceEngine engine = resolve(engineId);
        VoiceEngineTypeEnum type = VoiceEngineTypeEnum.of(engine.getEngineType());
        if (!"asr".equals(type.getKind())) {
            throw new CommonException("引擎实例不是 ASR 类型: " + engine.getEngineType());
        }
        long t0 = System.currentTimeMillis();
        asrConnectivityTester.test(engine);
        return System.currentTimeMillis() - t0;
    }

    /** 校验名称唯一 + 类型合法 */
    private void checkParams(VoiceEngineAddQuery query) {
        // 类型合法性校验（未注册类型直接报错，防脏数据）
        VoiceEngineTypeEnum.of(query.getEngineType());
        long count = count(new LambdaQueryWrapper<VoiceEngine>()
                .eq(VoiceEngine::getName, query.getName())
                .ne(Objects.nonNull(query.getId()), VoiceEngine::getId, query.getId())
                .eq(VoiceEngine::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (count > 0) {
            throw new CommonException("名称已存在！");
        }
    }

    /** config 按类型白名单过滤：只保留该类型 flatFields 内的 key，防前端塞无关/越权字段 */
    private String filterConfig(String engineType, String config) {
        if (StrUtil.isBlank(config)) {
            return "{}";
        }
        JSONObject parsed = JSON.parseObject(config);
        JSONObject filtered = new JSONObject();
        for (String field : VoiceEngineTypeEnum.of(engineType).getFlatFields()) {
            if (parsed.containsKey(field)) {
                filtered.put(field, parsed.get(field));
            }
        }
        return filtered.toJSONString();
    }

    private void evictCache(Long id) {
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("清除语音引擎缓存失败 id={}", id, e);
        }
    }
}
