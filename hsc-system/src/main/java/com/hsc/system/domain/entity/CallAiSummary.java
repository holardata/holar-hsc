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
 * 通话AI摘要扩展表(call_ai_summary)
 *
 * <p>挂断全文摘要：基于整通对话生成全文总结/关键词/角色总结/要点/代办/思维导图，
 * 按 call_id 唯一关联 CallRecord。各字段含独立 status(0-未请求 1-处理中 2-已完成)
 * 与 feedback(0-无 1-满意 2-不满意)标记，支持单字段重算与人工修正。
 * 平迁自 reminder_backend HistoryRecord(改名 call_ai_summary，字段语义不变)。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("call_ai_summary")
public class CallAiSummary extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "通话ID(关联 call_record.call_id)")
    @TableField("call_id")
    private String callId;

    @Schema(description = "全文总结")
    @TableField("summary")
    private String summary;

    @Schema(description = "关键词")
    @TableField("keyword_abstract")
    private String keywordAbstract;

    @Schema(description = "角色总结")
    @TableField("role_summary")
    private String roleSummary;

    @Schema(description = "要点提炼")
    @TableField("keypoint_extract")
    private String keypointExtract;

    @Schema(description = "代办事项")
    @TableField("todo_things")
    private String todoThings;

    @Schema(description = "思维导图JSON")
    @TableField("markdown_json")
    private String markdownJson;

    @Schema(description = "全文总结状态 0-未请求 1-处理中 2-已完成")
    @TableField("summary_status")
    private Integer summaryStatus;

    @Schema(description = "关键词状态 0-未请求 1-处理中 2-已完成")
    @TableField("keyword_status")
    private Integer keywordStatus;

    @Schema(description = "角色总结状态 0-未请求 1-处理中 2-已完成")
    @TableField("role_status")
    private Integer roleStatus;

    @Schema(description = "要点提炼状态 0-未请求 1-处理中 2-已完成")
    @TableField("keypoint_status")
    private Integer keypointStatus;

    @Schema(description = "代办事项状态 0-未请求 1-处理中 2-已完成")
    @TableField("todo_status")
    private Integer todoStatus;

    @Schema(description = "思维导图状态 0-未请求 1-处理中 2-已完成")
    @TableField("xmind_status")
    private Integer xmindStatus;

    @Schema(description = "关键词反馈 0-无 1-满意 2-不满意")
    @TableField("keyword_feedback")
    private Integer keywordFeedback;

    @Schema(description = "角色总结反馈 0-无 1-满意 2-不满意")
    @TableField("role_feedback")
    private Integer roleFeedback;

    @Schema(description = "要点提炼反馈 0-无 1-满意 2-不满意")
    @TableField("keypoint_feedback")
    private Integer keypointFeedback;

    @Schema(description = "代办事项反馈 0-无 1-满意 2-不满意")
    @TableField("todo_feedback")
    private Integer todoFeedback;

    @Schema(description = "思维导图反馈 0-无 1-满意 2-不满意")
    @TableField("xmind_feedback")
    private Integer xmindFeedback;

    @Schema(description = "全文总结反馈 0-无 1-满意 2-不满意")
    @TableField("summary_feedback")
    private Integer summaryFeedback;
}
