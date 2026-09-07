package com.hsc.system.util.tts;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.TtsEngineType;
import org.springframework.stereotype.Component;

/**
 * HolarTTS（kokoro）适配器（非流式 GET ?voice_id=&text=）。逻辑平移自 AiCallbotConfigServiceImpl#ttsHolar。
 * 前端传什么地址就请求什么，不裁剪/不追加。
 */
@Component
@TtsEngineType("holartts")
public class HolarTtsAdapter extends AbstractTtsEngineAdapter {

    @Override
    public TtsResult synthesize(JSONObject config, String text) {
        String url = requireConfig(config, "holartts_url", "Holartts 地址");
        HttpResponse resp = HttpRequest.get(url)
                .form("voice_id", cn.hutool.core.util.StrUtil.nullToEmpty(config.getString("holartts_voice_id")))
                .form("text", text)
                .timeout(HTTP_TIMEOUT_MS)
                .execute();
        byte[] bytes = resp.bodyBytes();
        ensureOk(resp, bytes, "HolartTS");
        return requireAudio(bytes, "HolartTS 返回空音频", "audio/wav");
    }
}
