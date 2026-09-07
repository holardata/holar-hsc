package com.hsc.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-256 摘要工具（JDK 原生实现）。
 * 统一入口供语音引擎参数指纹与 IVR 预合成缓存 key 等使用；
 * 不引 hutool-crypto（工程仅引 hutool-core/http 分模块）。
 */
public class DigestUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** SHA-256 → 小写 hex；入参 null 按空串处理 */
    public static String sha256Hex(String data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((data == null ? "" : data).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(HEX[(b >> 4) & 0xF]);
                sb.append(HEX[b & 0xF]);
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 为 JDK 必备算法，不会出现 NoSuchAlgorithmException
            throw new IllegalStateException("SHA-256 digest failed", e);
        }
    }
}
