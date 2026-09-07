package com.hsc.system.util.tts;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.TtsEngineType;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 火山引擎（豆包）TTS 适配器：HTTP 非流式合成（POST JSON + Bearer 鉴权，音频以 base64 返回）。
 * 接口为 openspeech /api/v1/tts（语音合成大模型批量合成）：
 * header Authorization: Bearer;{token}（火山特有分号分隔），body 按官方结构，
 * 成功响应 code=3000、data 为 base64 wav。
 */
@Component
@TtsEngineType("volc-tts")
public class VolcTtsAdapter extends AbstractTtsEngineAdapter {

    @Override
    public TtsResult synthesize(JSONObject config, String text) {
        String url = requireConfig(config, "volc_tts_url", "火山 TTS 地址");
        String token = requireConfig(config, "volc_tts_token", "火山 TTS 访问令牌");
        String voice = requireConfig(config, "volc_tts_voice", "火山 TTS 音色");

        JSONObject payload = new JSONObject();
        JSONObject app = new JSONObject();
        app.put("appid", StrUtil.nullToEmpty(config.getString("volc_tts_app_id")));
        app.put("token", token);
        app.put("cluster", StrUtil.nullToEmpty(config.getString("volc_tts_cluster")));
        payload.put("app", app);
        JSONObject user = new JSONObject();
        user.put("uid", "hsc-callcenter");
        payload.put("user", user);
        JSONObject audio = new JSONObject();
        audio.put("voice_type", voice);
        audio.put("encoding", "wav");
        audio.put("speed_ratio", config.containsKey("volc_tts_speed_ratio")
                ? config.getFloatValue("volc_tts_speed_ratio") : 1.0f);
        payload.put("audio", audio);
        payload.put("text", text);

        HttpResponse resp = HttpRequest.post(url)
                // 火山鉴权格式为 "Bearer;{token}"（分号），与通用 "Bearer {token}" 不同
                .header("Authorization", "Bearer;" + token)
                .body(payload.toJSONString(), "application/json")
                .timeout(HTTP_TIMEOUT_MS)
                .execute();
        byte[] bytes = resp.bodyBytes();
        ensureOk(resp, bytes, "火山 TTS");
        JSONObject body;
        try {
            body = JSON.parseObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("火山 TTS 返回非 JSON（前 100 字符: "
                    + StrUtil.brief(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), 100) + "）");
        }
        if (body == null || body.getIntValue("code") != 3000) {
            throw new RuntimeException("火山 TTS 合成失败 code=" + (body == null ? null : body.getIntValue("code"))
                    + " message=" + (body == null ? null : body.getString("message")));
        }
        String data = body.getString("data");
        if (StrUtil.isBlank(data)) {
            throw new RuntimeException("火山 TTS 返回空音频");
        }
        return new TtsResult(Base64.getDecoder().decode(data), "audio/wav");
    }
}
