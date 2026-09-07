package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 人工坐席助手配置表(human_agent_config，单例 id=1)。
 *
 * <p>holargpt 流式推荐话术的连接配置。平迁自 reminder_backend HumanAgentConfig。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("human_agent_config")
public class HumanAgentConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(单例固定1)")
    @TableId(type = IdType.INPUT)
    private Long id;

    @Schema(description = "holargpt基础地址")
    @TableField("holargpt_base_url")
    private String holargptBaseUrl;

    @Schema(description = "应用列表接口apiKey")
    @TableField("list_api_key")
    private String listApiKey;

    @Schema(description = "选用的智能体应用")
    @TableField("agent_selected_app")
    private String agentSelectedApp;

    @Schema(description = "智能体流式接口地址")
    @TableField("agent_api_url")
    private String agentApiUrl;

    @Schema(description = "智能体接口apiKey")
    @TableField("agent_api_key")
    private String agentApiKey;
}
