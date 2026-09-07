package com.hsc.system.util.aicallbot;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.VoiceEngine;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ASR 连通性测试：后端直连 ASR WebSocket 仅验证握手（不发业务消息），兼容 FunASR 等各类 ASR 服务。
 *
 * <p>平迁自 reminder_backend AsrConnectivityTester，保留 Java-WebSocket 实现：wss 时信任所有证书 +
 * 禁用主机名验证，兼容 IP 地址和自签名证书（JDK 原生 HttpClient 的 wss 无法可靠绕过这两项）。
 *
 * <p>unify-voice-engine-config：新增按语音引擎实例测试的重载 {@link #test(VoiceEngine)}——
 * 各云厂商 ASR 需带鉴权（query token / HMAC 签名）握手才能通过，按类型拼参后复用通用 WS 握手测试。
 */
@Slf4j
@Component
public class AsrConnectivityTester {

    /** 按引擎实例测试：按类型拼鉴权 URL 后做 WS 握手。延迟计量由调用方负责。 */
    public void test(VoiceEngine engine) {
        JSONObject config = engine.getConfig() == null ? new JSONObject()
                : JSON.parseObject(engine.getConfig());
        String wsUrl = buildAuthUrl(engine.getEngineType(), config);
        test(wsUrl);
    }

    /** 按类型拼鉴权 URL：funasr 裸连；aliyun 拼 appkey/token；tencent/xfyun 做 HMAC-SHA1 签名 */
    private String buildAuthUrl(String engineType, JSONObject c) {
        switch (engineType) {
            case "funasr" -> {
                return requireUrl(c.getString("local_asr_url"));
            }
            case "aliyun-nls" -> {
                String url = requireUrl(c.getString("aliyun_asr_url"));
                String sep = url.contains("?") ? "&" : "?";
                return url + sep + "appkey=" + enc(c.getString("aliyun_asr_appkey"))
                        + "&token=" + enc(c.getString("aliyun_asr_token"));
            }
            case "tencent-asr" -> {
                // 腾讯实时识别 V2：signature = base64(hmac_sha1(secret_key, "GET" + host + path + "?" + 排序参数串))
                String url = requireUrl(c.getString("tencent_asr_url")); // 形如 wss://asr.cloud.tencent.com/asr/v2/16k_zh-D
                long ts = System.currentTimeMillis() / 1000;
                Map<String, String> params = new TreeMap<>();
                params.put("secretid", c.getString("tencent_asr_secret_id"));
                params.put("timestamp", String.valueOf(ts));
                params.put("expired", String.valueOf(ts + 3600));
                params.put("nonce", String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999)));
                params.put("voice_type", "0");
                params.put("format", "2");
                params.put("needhotword", "0");
                URI uri = URI.create(url);
                String host = uri.getHost();
                String path = uri.getPath() == null ? "" : uri.getPath();
                StringBuilder sorted = new StringBuilder();
                for (Map.Entry<String, String> e : params.entrySet()) {
                    if (!sorted.isEmpty()) {
                        sorted.append('&');
                    }
                    sorted.append(e.getKey()).append('=').append(enc(e.getValue()));
                }
                String signSrc = "GET" + host + path + "?" + sorted;
                String signature = hmacSha1Base64(c.getString("tencent_asr_secret_key"), signSrc);
                return url + (url.contains("?") ? "&" : "?") + sorted + "&signature=" + enc(signature);
            }
            case "xfyun-asr" -> {
                // 讯飞 RTASR：signa = base64(hmac_sha1(api_key, appid + ts))，ts 秒级
                String url = requireUrl(c.getString("xfyun_asr_url")); // 形如 wss://rtasr.xfyun.cn/v1/ws
                String appId = c.getString("xfyun_asr_app_id");
                String ts = String.valueOf(System.currentTimeMillis() / 1000);
                String signa = hmacSha1Base64(c.getString("xfyun_asr_api_key"), appId + ts);
                return url + (url.contains("?") ? "&" : "?") + "appid=" + enc(appId)
                        + "&ts=" + ts + "&signa=" + enc(signa);
            }
            default -> throw new CommonException("该引擎类型不支持 ASR 连通性测试: " + engineType);
        }
    }

    private String requireUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new CommonException("ASR 地址未配置");
        }
        return url.trim();
    }

    private String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private String hmacSha1Base64(String key, String data) {
        if (key == null || key.isEmpty()) {
            throw new CommonException("签名密钥未配置");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new CommonException("签名计算失败: " + e.getMessage());
        }
    }

    public void test(String wsUrl) {
        String resolved = resolveForWebSocketClient(wsUrl);
        if (resolved.isEmpty()) {
            throw new CommonException("WebSocket 地址不能为空");
        }
        URI uri;
        try {
            uri = new URI(resolved);
        } catch (Exception e) {
            throw new CommonException("无效的 WebSocket 地址");
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("ws") && !scheme.equalsIgnoreCase("wss"))) {
            throw new CommonException("地址必须是 ws:// 或 wss://");
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>(null);
        AtomicReference<Boolean> opened = new AtomicReference<>(false);

        WebSocketClient client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                opened.set(true);
                latch.countDown();
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (error.get() == null) {
                    error.set("连接失败，请检查地址是否正确");
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                log.warn("ASR 测试连接错误 url={}", resolved, ex);
                if (error.get() == null) {
                    error.set("连接失败，请检查地址是否正确");
                }
                latch.countDown();
            }

            @Override
            protected void onSetSSLParameters(SSLParameters sslParameters) {
                // 禁用主机名验证，兼容 IP 地址和自签名证书
                sslParameters.setEndpointIdentificationAlgorithm(null);
            }
        };

        try {
            // wss 自签名证书信任
            if ("wss".equalsIgnoreCase(uri.getScheme())) {
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, null);
                client.setSocketFactory(ssl.getSocketFactory());
            }

            client.setConnectionLostTimeout(5);
            client.connectBlocking(5, TimeUnit.SECONDS);

            boolean ok = latch.await(6, TimeUnit.SECONDS);
            try {
                client.close();
            } catch (Exception ignored) {
                // 忽略关闭异常
            }

            if (!ok || error.get() != null || !opened.get()) {
                throw new CommonException("ASR 连接失败，请检查服务地址是否正确");
            }
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException("ASR 连接失败：" + e.getMessage());
        }
    }

    /** 将配置中的 ASR WebSocket 地址解析为 Java-WebSocket 可用的绝对 URI（协议相对地址按 ws: 处理）。 */
    private static String resolveForWebSocketClient(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.startsWith("//")) {
            return "ws:" + s;
        }
        return s;
    }
}
