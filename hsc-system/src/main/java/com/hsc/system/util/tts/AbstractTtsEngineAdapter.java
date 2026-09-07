package com.hsc.system.util.tts;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.util.StrUtil;

/**
 * TTS 适配器公共基类：HTTP 状态与空音频校验（ensureOk/requireAudio 平移自原
 * AiCallbotConfigServiceImpl，四个既有引擎行为一致）。
 */
public abstract class AbstractTtsEngineAdapter implements TtsEngineAdapter {

    protected static final int HTTP_TIMEOUT_MS = 30_000;

    /** 非 2xx 抛错并带状态码/响应大小，避免服务端报错被笼统报成"返回空音频" */
    protected void ensureOk(HttpResponse resp, byte[] bytes, String label) {
        if (!resp.isOk()) {
            throw new RuntimeException(label + " 服务返回 HTTP " + resp.getStatus()
                    + "（响应 " + (bytes == null ? 0 : bytes.length) + " 字节）");
        }
    }

    protected TtsResult requireAudio(byte[] bytes, String emptyMsg, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new RuntimeException(emptyMsg);
        }
        return new TtsResult(bytes, contentType);
    }

    /** 取必填参数，缺失时抛可读错误 */
    protected String requireConfig(JSONObject c, String key, String label) {
        String v = StrUtil.nullToEmpty(c.getString(key)).trim();
        if (StrUtil.isBlank(v)) {
            throw new RuntimeException(label + "未配置（字段 " + key + "）");
        }
        return v;
    }
}
