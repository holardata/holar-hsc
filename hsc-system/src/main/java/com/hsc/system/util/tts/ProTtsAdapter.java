package com.hsc.system.util.tts;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.TtsEngineType;
import org.springframework.stereotype.Component;

/**
 * 专业版 IndexTTS 适配器（GET + form）。逻辑平移自 AiCallbotConfigServiceImpl#ttsPro。
 * 前端传什么地址就请求什么，不裁剪/不追加（避免配置与实际请求不一致让用户困惑）。
 */
@Component
@TtsEngineType("pro")
public class ProTtsAdapter extends AbstractTtsEngineAdapter {

    @Override
    public TtsResult synthesize(JSONObject config, String text) {
        String url = requireConfig(config, "local_tts_pro_url", "专业版 TTS 地址");
        HttpResponse resp = HttpRequest.get(url)
                .form("text", text)
                .form("audio_paths", cn.hutool.core.util.StrUtil.nullToEmpty(config.getString("local_tts_audio_path")))
                .timeout(HTTP_TIMEOUT_MS)
                .execute();
        byte[] bytes = resp.bodyBytes();
        ensureOk(resp, bytes, "专业版 TTS");
        return requireAudio(bytes, "专业版 TTS 返回空音频", "audio/wav");
    }
}
