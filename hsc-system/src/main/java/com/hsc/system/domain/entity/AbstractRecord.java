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
 * 通话过程摘要表(abstract_record)
 *
 * <p>过程摘要 Quartz 定时任务(每 2 分钟)对进行中通话生成的阶段性摘要，按 call_id 关联 CallRecord。
 * 平迁自 reminder_backend AbstractRecord。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("abstract_record")
public class AbstractRecord extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "通话ID")
    @TableField("call_id")
    private String callId;

    @Schema(description = "原文快照")
    @TableField("dialog_txt")
    private String dialogTxt;

    @Schema(description = "标题摘要")
    @TableField("title")
    private String title;

    @Schema(description = "内容摘要")
    @TableField("content")
    private String content;

    @Schema(description = "反馈标记 0-无 1-满意 2-不满意")
    @TableField("feedback")
    private Integer feedback;

    @Schema(description = "反馈文本")
    @TableField("feedback_txt")
    private String feedbackTxt;
}
