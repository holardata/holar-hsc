package com.hsc.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.ai.client.OpenAiCompatibleClientFactory;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.ModelConfig;
import com.hsc.system.domain.query.ai.ModelConfigAddQuery;
import com.hsc.system.domain.query.ai.ModelConfigQuery;
import com.hsc.system.mapper.ModelConfigMapper;
import com.hsc.system.service.IModelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM模型配置(model_config)表服务实现。
 *
 * <p>连通性测试(test)与默认模型配置组装(buildConf)经 {@link OpenAiCompatibleClientFactory}，
 * 遵循"LLM 调用统一走 hsc-ai"约束。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ModelConfigServiceImpl extends BaseServiceImpl<ModelConfigMapper, ModelConfig> implements IModelConfigService {

    private final OpenAiCompatibleClientFactory openAiCompatibleClientFactory;

    @Override
    public ModelConfig getDefaultModel() {
        return getOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getIsDefault, 1)
                .eq(ModelConfig::getStatus, 1)
                .last("limit 1"));
    }

    @Override
    public PageInfo<ModelConfig> getPageList(ModelConfigQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<ModelConfig> list = list(new LambdaQueryWrapper<ModelConfig>()
                .like(StrUtil.isNotBlank(query.getName()), ModelConfig::getName, query.getName())
                .like(StrUtil.isNotBlank(query.getBaseModel()), ModelConfig::getBaseModel, query.getBaseModel())
                .eq(query.getStatus() != null, ModelConfig::getStatus, query.getStatus())
                .eq(query.getIsDefault() != null, ModelConfig::getIsDefault, query.getIsDefault())
                .orderByDesc(ModelConfig::getId));
        return new PageInfo<>(list);
    }

    @Override
    public void add(ModelConfigAddQuery query) {
        ModelConfig model = new ModelConfig();
        BeanUtil.copyProperties(query, model);
        if (query.getConfig() != null) {
            model.setConfig(new JSONObject(query.getConfig()));
        }
        save(model);
    }

    @Override
    public void update(ModelConfigAddQuery query) {
        ModelConfig old = getById(query.getId());
        if (old == null) {
            throw new CommonException("模型配置不存在");
        }
        // baseUrl/apiKey 可能变更，清理旧客户端缓存使新配置下次生效
        evictClient(old);
        ModelConfig model = new ModelConfig();
        BeanUtil.copyProperties(query, model);
        if (query.getConfig() != null) {
            model.setConfig(new JSONObject(query.getConfig()));
        }
        updateById(model);
    }

    @Override
    public void delete(ModelConfigQuery query) {
        List<Long> ids = query.getIds();
        if ((ids == null || ids.isEmpty()) && query.getId() != null) {
            ids = Collections.singletonList(query.getId());
        }
        if (ids == null || ids.isEmpty()) {
            throw new CommonException("请选择要删除的模型");
        }
        List<ModelConfig> models = listByIds(ids);
        if (models.stream().anyMatch(m -> m.getIsDefault() != null && m.getIsDefault() == 1)) {
            throw new CommonException("默认模型不能删除，请先取消默认");
        }
        models.forEach(this::evictClient);
        ModelConfig del = new ModelConfig();
        del.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        update(del, new LambdaQueryWrapper<ModelConfig>().in(ModelConfig::getId, ids));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setDefault(Long id) {
        ModelConfig model = getById(id);
        if (model == null) {
            throw new CommonException("模型配置不存在");
        }
        // 先把所有默认置为非默认，再设当前为默认（互斥）
        ModelConfig reset = new ModelConfig();
        reset.setIsDefault(0);
        update(reset, new LambdaQueryWrapper<ModelConfig>().eq(ModelConfig::getIsDefault, 1));
        ModelConfig cur = new ModelConfig();
        cur.setId(id);
        cur.setIsDefault(1);
        updateById(cur);
    }

    @Override
    public void setEnabled(Long id, Integer status) {
        ModelConfig model = new ModelConfig();
        model.setId(id);
        model.setStatus(status);
        updateById(model);
    }

    @Override
    public long test(Long id) {
        ModelConfig model = getById(id);
        if (model == null) {
            throw new CommonException("模型配置不存在");
        }
        Map<String, Object> conf = buildConf(model);
        long start = System.currentTimeMillis();
        try {
            openAiCompatibleClientFactory.testChatCompletion(conf, "你好", 50, 60);
        } catch (Exception e) {
            log.error("模型连通性测试失败 id={}, name={}, err={}", id, model.getName(), e.getMessage(), e);
            throw new CommonException("模型连通性测试失败：" + e.getMessage());
        }
        return System.currentTimeMillis() - start;
    }

    @Override
    public Map<String, Object> buildConf(ModelConfig model) {
        Map<String, Object> conf = new HashMap<>();
        conf.put("model", model.getBaseModel());
        conf.put("base_url", model.getApiDomain());
        // config(JSONObject, implements Map) 含 api_key/temperature/max_tokens 等，整体并入 conf
        if (model.getConfig() != null) {
            conf.putAll(model.getConfig());
        }
        return conf;
    }

    /** baseUrl/apiKey 变更或删除时，清理对应的同步客户端缓存。 */
    private void evictClient(ModelConfig model) {
        if (model == null || StrUtil.isBlank(model.getApiDomain())) {
            return;
        }
        String apiKey = model.getConfig() == null ? null : model.getConfig().getString("api_key");
        openAiCompatibleClientFactory.evictSyncClient(apiKey, model.getApiDomain());
    }
}
