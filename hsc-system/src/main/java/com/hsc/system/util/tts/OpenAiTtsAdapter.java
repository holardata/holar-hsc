package com.hsc.system.util.tts;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.TtsEngineType;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容 TTS 适配器（POST JSON /v1/audio/speech，qwen/cosyvoice/fish 等自建服务通用）。
 * 逻辑平移自 AiCallbotConfigServiceImpl#ttsLocal（原 tts_engine=local）。
 */
@Component
@TtsEngineType("openai-tts")
public class OpenAiTtsAdapter extends AbstractTtsEngineAdapter {

    @Override
    public TtsResult synthesize(JSONObject config, String text) {
        String url = requireConfig(config, "tts_local_url", "本地 TTS 地址");
        JSONObject payload = new JSONObject();
        payload.put("input", text);
        payload.put("voice", config.getString("tts_local_voice"));
        payload.put("instructions", config.getString("tts_local_instructions"));
        payload.put("response_format", "wav");
        HttpResponse resp = HttpRequest.post(url)
                .body(payload.toJSONString(), "application/json")
                .timeout(HTTP_TIMEOUT_MS)
                .execute();
        byte[] bytes = resp.bodyBytes();
        ensureOk(resp, bytes, "本地 TTS");
        return requireAudio(bytes, "本地 TTS 返回空音频", "audio/wav");
    }
}
