// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.config.oss;

import lombok.Data;

/**
 * 腾讯云存储配置
 */
@Data
public class TxCloudConfig {


    /**
     * 腾讯云存储
     */
    private TxCosConfig cos;


    @Data
    public static class TxCosConfig {


        /**
         * 腾讯云域名
         */
        private String host;
        /**
         * 桶名称
         */
        private String bucketName;

        /**
         * 地区
         */
        private String regionName;

        /**
         * 密钥key
         */
        private String accessKey;

        /**
         * 密钥
         */
        private String secretKey;
    }
}
