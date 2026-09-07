package com.hsc.system.domain.entity;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import com.hsc.system.handler.JsonObjectTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * LLM模型配置表(model_config)
 *
 * <p>运营管理的动态 LLM 配置：name/base_model(上游模型名)/api_domain(OpenAI 兼容 baseUrl)
 * + config(附加参数 JSON：api_key/temperature/max_tokens 等)。is_default=1 的模型供摘要等
 * 业务默认使用。平迁自 reminder_backend ModelConfig。
 *
 * <p>config 用 {@link JsonObjectTypeHandler} 映射为 fastjson2 {@link JSONObject}，
 * 它 implements {@link java.util.Map}，可直接作为 conf 传入 OpenAiCompatibleClientFactory。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName(value = "model_config", autoResultMap = true)
public class ModelConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "模型配置名称")
    @TableField("name")
    private String name;

    @Schema(description = "基础模型名(如 qwen-plus/deepseek-chat)")
    @TableField("base_model")
    private String baseModel;

    @Schema(description = "OpenAI兼容baseUrl")
    @TableField("api_domain")
    private String apiDomain;

    @Schema(description = "附加配置JSON(api_key/temperature/max_tokens等)")
    @TableField(value = "config", typeHandler = JsonObjectTypeHandler.class)
    private JSONObject config;

    @Schema(description = "是否默认 0-否 1-是")
    @TableField("is_default")
    private Integer isDefault;

    @Schema(description = "状态 0-停用 1-启用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
