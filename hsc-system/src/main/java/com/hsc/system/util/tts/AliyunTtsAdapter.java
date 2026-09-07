package com.hsc.system.util.tts;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.TtsEngineType;
import org.springframework.stereotype.Component;

/**
 * 阿里云 NLS TTS 适配器（GET + form 参数）。逻辑平移自 AiCallbotConfigServiceImpl#ttsAliyun。
 */
@Component
@TtsEngineType("aliyun-tts")
public class AliyunTtsAdapter extends AbstractTtsEngineAdapter {

    @Override
    public TtsResult synthesize(JSONObject config, String text) {
        String url = requireConfig(config, "aliyun_tts_url", "阿里云 TTS 地址");
        HttpResponse resp = HttpRequest.get(url)
                .form("appkey", config.getString("aliyun_tts_appkey"))
                .form("token", config.getString("aliyun_tts_token"))
                .form("text", text)
                .form("format", config.getString("aliyun_tts_format"))
                .form("sample_rate", config.getIntValue("aliyun_tts_sample_rate"))
                .form("voice", config.getString("aliyun_tts_voice"))
                .timeout(HTTP_TIMEOUT_MS)
                .execute();
        byte[] bytes = resp.bodyBytes();
        ensureOk(resp, bytes, "阿里云 TTS");
        String fmt = StrUtil.nullToEmpty(config.getString("aliyun_tts_format"));
        return requireAudio(bytes, "阿里云 TTS 返回空音频",
                "mp3".equalsIgnoreCase(fmt) ? "audio/mpeg" : "audio/wav");
    }
}
