package com.hsc.system.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;

/**
 * HolarGPT 智能体平台列表统一调用工具（平迁自 reminder_backend HolarGptListClient）。
 *
 * <p>供 {@code HumanAgentConfigServiceImpl}（坐席助手应用列表）与 {@code AiCallbotConfigServiceImpl}
 * （AI 坐席应用/知识库列表）共用，避免重复 HTTP 逻辑。请求 {@code GET {base}{endpoint}}，
 * {@code Authorization: Bearer {apiKey}} 鉴权，返回外层 {@code data} 字段。
 *
 * <p><b>HTTP 客户端用 hutool {@link HttpRequest}（HTTP/1.1），与老项目 reminder_backend 完全一致。</b>
 * 切勿换回 JDK {@code java.net.http.HttpClient}：其默认 HTTP/2（明文下走 h2c upgrade），会让 holargpt
 * 的 nginx 返回 502 Bad Gateway（老项目 hutool HTTP/1.1 正常、宿主机 curl HTTP/1.1 正常，唯独 JDK
 * HttpClient HTTP/2 被 holargpt nginx 502）。
 *
 * <p>错误处理：在老项目基础上加 HTTP 状态码 + 空响应 + JSON 可解析性校验，非 JSON 响应（如 nginx
 * 502 HTML 错误页）抛可读提示而非 fastjson 内部报错，便于定位"服务地址不可达"。
 */
@Slf4j
public final class HolarGptListClient {

    /** 请求超时（毫秒），对齐老项目 10s */
    private static final int TIMEOUT_MS = 10_000;

    /** 非预期响应体的预览最大长度（避免把整段 HTML 错误页塞进提示） */
    private static final int PREVIEW_MAX = 120;

    private HolarGptListClient() {
    }

    /**
     * 调用 HolarGPT 列表接口，返回外层 {@code data} 字段。
     *
     * @param base     holargpt 服务地址（如 http://18.18.18.18:13456），为空抛 CommonException
     * @param apiKey   系统级 apiKey（裸值，方法内自动补 "Bearer "）
     * @param endpoint 接口路径（如 /api/core/app/list、/api/core/dataset/list）
     * @param label    场景标签（如"智能体列表"），用于拼装可读错误信息
     * @return holargpt 响应的 data 字段（通常为数组），失败时抛 CommonException
     */
    public static Object list(String base, String apiKey, String endpoint, String label) {
        if (StrUtil.isBlank(base)) {
            throw new CommonException(label + "请求失败：HolarGPT 服务地址未配置，请先在配置页保存");
        }
        String url = base.replaceAll("/+$", "") + endpoint;
        String token = apiKey == null ? "" : apiKey;
        try {
            HttpResponse resp = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT_MS)
                    .execute();
            int status = resp.getStatus();
            String body = resp.body();
            if (status != 200) {
                log.warn("{} 请求非 200 url={}, status={}, body={}", label, url, status, body);
                throw new CommonException(
                        label + "请求失败：智能体平台返回 HTTP " + status + "，请检查服务地址是否可达");
            }
            if (StrUtil.isBlank(body)) {
                throw new CommonException(label + "请求失败：智能体平台返回空响应");
            }
            try {
                JSONObject json = JSON.parseObject(body);
                return json == null ? null : json.get("data");
            } catch (JSONException jsonEx) {
                String preview = body.length() > PREVIEW_MAX
                        ? body.substring(0, PREVIEW_MAX) + "..." : body;
                log.warn("{} 请求返回非 JSON 响应 url={}, body={}", label, url, body, jsonEx);
                throw new CommonException(label + "请求失败：智能体平台返回非 JSON 响应（HTTP " + status
                        + "），可能为错误页，请检查服务地址。响应预览：" + preview);
            }
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("{} 请求异常 url={}", label, url, e);
            String em = e.getMessage();
            throw new CommonException(label + "请求失败："
                    + (StrUtil.isNotBlank(em) ? em : "无法连接智能体平台，请检查服务地址是否可达"));
        }
    }
}
