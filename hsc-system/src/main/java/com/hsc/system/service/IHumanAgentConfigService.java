package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.HumanAgentConfig;

/**
 * 人工坐席助手配置(human_agent_config)表服务接口（单例 id=1）
 */
public interface IHumanAgentConfigService extends IBaseService<HumanAgentConfig> {

    /** 获取单例配置(id=1)；不存在返回 null。 */
    HumanAgentConfig getSingle();

    /** 保存或更新单例配置(id 固定为1)。 */
    void saveOrUpdateSingle(HumanAgentConfig config);

    /**
     * 查询 holargpt 智能体应用列表（调 /api/core/app/list，供配置页下拉）。
     * 平迁自 reminder_backend HolarGptListClient。
     *
     * @return holargpt 响应的 data 字段；未配置 base_url 抛 CommonException
     */
    Object listApps();
}
