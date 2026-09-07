// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.config.oss;

import lombok.Data;

/**
 * 阿里云存储配置
 */
@Data
public class AliCloudConfig {
    /**
     * 阿里云存储
     */
    private AliCosConfig cos;


    @Data
    public static class AliCosConfig {
        /**
         * 阿里云域名
         */
        private String host;

        /**
         * 域名
         */
        private String endpoint;

        /**
         * 桶名称
         */
        private String bucketName;

        /**
         * 桶区域
         */
        private String region;

        /**
         * 应用ID
         */
        private String accessKeyId;
        /**
         * 应用密钥
         */
        private String accessKeySecret;
    }
}
