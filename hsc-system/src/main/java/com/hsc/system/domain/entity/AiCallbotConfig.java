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
 * AI 坐席配置表(ai_callbot_config，单例 id=1)。
 *
 * <p>AI 自动接听电话的全量运行参数，configJson 整段承载（对齐 ai-callbot RuntimeConfig：
 * ASR/TTS/知识库/智能体/转人工/欢迎语/对话行为/VAD/静音挂断）。供 ai-callbot 每通电话拉取。
 * 平迁自 reminder_backend AiCallbotConfig（rb 用 configJson 整串 + updatedBy；hsc 用 BaseEntity 审计字段，
 * 故不单独保留 updatedBy，改用 update_by）。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("ai_callbot_config")
public class AiCallbotConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID(单例固定1)")
    @TableId(type = IdType.INPUT)
    private Long id;

    @Schema(description = "完整业务配置JSON(对齐 ai-callbot RuntimeConfig 全字段)")
    @TableField("config_json")
    private String configJson;
}
