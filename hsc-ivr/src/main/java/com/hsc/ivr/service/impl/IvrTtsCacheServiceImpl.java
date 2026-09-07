package com.hsc.ivr.service.impl;

import cn.hutool.core.io.FileUtil;
import com.hsc.common.constant.SysSettingConfig;
import com.hsc.common.utils.DigestUtils;
import com.hsc.system.service.IVoiceEngineService;
import com.hsc.system.util.tts.TtsResult;
import com.hsc.ivr.service.IvrTtsCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * IVR 文本放音预合成缓存实现（unify-voice-engine-config）。
 *
 * <p>缓存目录：{system.setting.baseProfile}/voice/tts-cache/，与语音文件同挂载源（后端 /temp/voice
 * = FS /usr/local/freeswitch/share/freeswitch/sounds）。返回的 "tts-cache/xxx.wav" 相对路径由
 * FsClient 下发 playback 时统一拼 FS 绝对路径（自编译 FS 相对路径解析不可依赖）。
 * 缓存 key = sha256(engineId + fingerprint + 渲染后文本) 前 32 位：文本变/引擎改参自动换 key，
 * 不过期不清理（磁盘可容忍；引擎实例删除后缓存文件残留无害）。
 * 并发写同 key 文件为覆盖写同内容，竞争无害。
 */
@Slf4j
@Service
public class IvrTtsCacheServiceImpl implements IvrTtsCacheService {

    private static final String CACHE_DIR_NAME = "tts-cache";

    @Resource
    private SysSettingConfig sysSettingConfig;

    @Resource
    private IVoiceEngineService voiceEngineService;

    @Override
    public String synthToCacheFile(Long engineId, String fingerprint, String renderedText) {
        if (engineId == null) {
            log.warn("IVR 文本放音未配置 TTS 引擎实例（开始节点未选引擎），本次跳过合成");
            return null;
        }
        try {
            String key = DigestUtils.sha256Hex(engineId + ":" + (fingerprint == null ? "" : fingerprint)
                    + ":" + renderedText).substring(0, 32);
            File dir = new File(sysSettingConfig.getBaseProfile() + "/voice/" + CACHE_DIR_NAME + "/");
            // 命中：前缀匹配（wav/mp3 均可能）
            File[] hits = dir.listFiles((d, name) -> name.startsWith(key + "."));
            if (hits != null && hits.length > 0) {
                return CACHE_DIR_NAME + "/" + hits[0].getName();
            }
            // 未命中：统一入口合成落盘
            TtsResult result = voiceEngineService.synthesizeByEngineId(engineId, renderedText);
            String suffix = "audio/mpeg".equalsIgnoreCase(result.getContentType()) ? "mp3" : "wav";
            String fileName = key + "." + suffix;
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("IVR TTS 缓存目录创建失败: {}", dir.getAbsolutePath());
                return null;
            }
            FileUtil.writeBytes(result.getBody(), new File(dir, fileName));
            log.info("IVR 文本放音预合成完成 engineId={} file={}/{}", engineId, CACHE_DIR_NAME, fileName);
            return CACHE_DIR_NAME + "/" + fileName;
        } catch (Exception e) {
            // 容错：合成失败不挂死流程，调用方以 silence 兜底跳过放音
            log.error("IVR 文本放音预合成失败 engineId={} text={}", engineId,
                    renderedText == null ? "" : renderedText.substring(0, Math.min(50, renderedText.length())), e);
            return null;
        }
    }
}
