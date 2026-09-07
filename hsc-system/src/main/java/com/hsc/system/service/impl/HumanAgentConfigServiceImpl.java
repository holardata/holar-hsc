package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.HumanAgentConfig;
import com.hsc.system.mapper.HumanAgentConfigMapper;
import com.hsc.system.service.IHumanAgentConfigService;
import com.hsc.system.util.HolarGptListClient;
import org.springframework.stereotype.Service;

/**
 * 人工坐席助手配置(human_agent_config)表服务实现（单例 id=1）。
 *
 * <p>listApps 调 holargpt {@code /api/core/app/list} 取智能体应用列表（配置页下拉），
 * HTTP/解析逻辑收口于 {@link HolarGptListClient}（与 AiCallbotConfigServiceImpl 共用）。
 */
@Service
public class HumanAgentConfigServiceImpl extends BaseServiceImpl<HumanAgentConfigMapper, HumanAgentConfig> implements IHumanAgentConfigService {

    private static final long SINGLE_ID = 1L;

    @Override
    public HumanAgentConfig getSingle() {
        return getById(SINGLE_ID);
    }

    @Override
    public void saveOrUpdateSingle(HumanAgentConfig config) {
        config.setId(SINGLE_ID);
        saveOrUpdate(config);
    }

    @Override
    public Object listApps() {
        HumanAgentConfig config = getSingle();
        String base = config == null ? null : config.getHolargptBaseUrl();
        String apiKey = config == null ? null : config.getListApiKey();
        return HolarGptListClient.list(base, apiKey, "/api/core/app/list", "坐席助手智能体列表");
    }
}
