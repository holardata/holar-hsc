// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.config;

import com.hsc.esl.client.FsClient;
import com.hsc.esl.propeties.FsClientProperties;
import com.hsc.system.service.IFsConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author danmo
 * @date 2023年06月28日 18:36
 */
@Slf4j
@Configuration
public class FsClientConfig {

    /**
     * 初始话ESL连接
     *
     * @return
     */
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public FsClient fsClient(IFsConfigService iFsConfigService, FsClientProperties clientProperties) {
        return new FsClient(iFsConfigService,clientProperties);
    }

}
