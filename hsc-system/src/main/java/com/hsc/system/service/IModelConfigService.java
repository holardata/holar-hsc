package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.ModelConfig;
import com.hsc.system.domain.query.ai.ModelConfigAddQuery;
import com.hsc.system.domain.query.ai.ModelConfigQuery;

import java.util.Map;

/**
 * LLM模型配置(model_config)表服务接口
 */
public interface IModelConfigService extends IBaseService<ModelConfig> {

    /**
     * 获取启用的默认模型(is_default=1 且 status=1)。
     *
     * @return 默认模型；不存在返回 null
     */
    ModelConfig getDefaultModel();

    /**
     * 分页查询模型配置列表。
     */
    PageInfo<ModelConfig> getPageList(ModelConfigQuery query);

    /**
     * 新增模型配置。
     */
    void add(ModelConfigAddQuery query);

    /**
     * 修改模型配置。
     */
    void update(ModelConfigAddQuery query);

    /**
     * 删除模型配置(逻辑删除)；默认模型不允许删除。
     */
    void delete(ModelConfigQuery query);

    /**
     * 设为默认模型(其余自动置为非默认)。
     */
    void setDefault(Long id);

    /**
     * 启用/停用模型。
     *
     * @param status 0-停用 1-启用
     */
    void setEnabled(Long id, Integer status);

    /**
     * 连通性测试：用该模型配置发一条短消息，返回响应延迟(ms)；失败抛 CommonException。
     */
    long test(Long id);

    /**
     * 将模型配置组装为 OpenAI 兼容客户端的 conf Map(model/base_url + config 附加参数)。
     * 摘要网关与连通性测试共用此组装，避免重复逻辑。
     */
    Map<String, Object> buildConf(ModelConfig model);
}
