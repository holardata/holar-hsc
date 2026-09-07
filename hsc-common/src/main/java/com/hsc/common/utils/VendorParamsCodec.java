package com.hsc.common.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Vendor 参数 ASCII 三位编解码工具。
 *
 * <p>ai-callbot 推送 Redis 队列（{@code websocket:message:queue}）的消息形如：
 * <pre>{@code
 * encodedVendorParams|asrJson
 * }</pre>
 * 其中 encodedVendorParams 是把形如 {@code role=user&callId=xxx&agentId=yyy&userNumber=zzz&source=bypass}
 * 的参数串按"每字符 ASCII 三位十进制"编码得到的纯数字串（{@code '&'}(38) 编码为 {@code "038"}）。
 *
 * <p>本工具与 ai-callbot {@code redis_publisher.encode_vendor_params} 严格一致（跨工程协议契约）。
 * 平迁自 reminder_backend VendorParamsEncoder / VendorParamsDecoder，合并为单一工具类。
 */
public final class VendorParamsCodec {

    private VendorParamsCodec() {
    }

    /** Redis 消息中"编码参数"与"消息体"的分隔符。 */
    public static final String MSG_SEPARATOR = "|";

    /** 参数键：发言人角色 user/agent。 */
    public static final String KEY_ROLE = "role";
    /** 参数键：通话ID（= hsc CallRecord.callId）。 */
    public static final String KEY_CALL_ID = "callId";
    /** 参数键：坐席工号（= SipAgent.agentNumber）或 AIBOT。 */
    public static final String KEY_AGENT_ID = "agentId";
    /** 参数键：客户号码。 */
    public static final String KEY_USER_NUMBER = "userNumber";
    /** 参数键：来源 ai/bypass/asr_proxy。 */
    public static final String KEY_SOURCE = "source";

    /**
     * 将参数串编码为 ASCII 三位纯数字串。
     *
     * @param paramsString 原始参数串，如 {@code role=user&callId=xxx}
     * @return 编码后的纯数字串；入参空返回 {@code ""}
     */
    public static String encode(String paramsString) {
        if (paramsString == null || paramsString.isEmpty()) {
            return "";
        }
        StringBuilder encoded = new StringBuilder(paramsString.length() * 3);
        for (int i = 0; i < paramsString.length(); i++) {
            char c = paramsString.charAt(i);
            // '&' 分隔符编码为 "038"（其 ASCII=38，与 %03d 结果一致；显式处理保持协议清晰）
            if (c == '&') {
                encoded.append("038");
            } else {
                encoded.append(String.format("%03d", (int) c));
            }
        }
        return encoded.toString();
    }

    /**
     * 将参数 Map 按 {@code key=value&key=value} 拼装后编码。
     */
    public static String encode(Map<String, String> paramsMap) {
        if (paramsMap == null || paramsMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : paramsMap.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        return encode(sb.toString());
    }

    /**
     * 将 ASCII 三位纯数字串解码为原始参数串。
     *
     * @param encodedString 编码串
     * @return 原始参数串；入参空返回 {@code null}；长度非 3 的倍数抛 {@link IllegalArgumentException}
     */
    public static String decode(String encodedString) {
        if (encodedString == null || encodedString.isEmpty()) {
            return null;
        }
        if (encodedString.length() % 3 != 0) {
            throw new IllegalArgumentException("编码字符串长度必须是3的倍数");
        }
        StringBuilder decoded = new StringBuilder(encodedString.length() / 3);
        for (int i = 0; i < encodedString.length(); i += 3) {
            int charCode = Integer.parseInt(encodedString.substring(i, i + 3));
            decoded.append((char) charCode);
        }
        return decoded.toString();
    }

    /**
     * 将编码串解码并解析为 {@code key=value} Map（重复 key 保留第一个）。
     *
     * @param encodedString 编码串
     * @return 参数 Map；入参空返回空 Map
     */
    public static Map<String, String> parseParams(String encodedString) {
        String decoded = decode(encodedString);
        if (decoded == null || decoded.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> params = new HashMap<>();
        for (String pair : decoded.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq);
                String value = pair.substring(eq + 1);
                if (!params.containsKey(key)) {
                    params.put(key, value);
                }
            }
        }
        return params;
    }
}
