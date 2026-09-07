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
 * 智能体提示词表(chat_promptword)
 *
 * <p>各业务场景的提示词(过程摘要/全文总结/关键词/角色总结/要点/代办/思维导图/AI翻译/AI改写/JSON优化等)。
 * 摘要业务按 title 从该表读取提示词，不硬编码。title 唯一。
 * 平迁自 reminder_backend ChatPromptword。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("chat_promptword")
public class ChatPromptword extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "提示词标题(过程摘要/全文总结等)")
    @TableField("title")
    private String title;

    @Schema(description = "提示词内容")
    @TableField("content")
    private String content;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
