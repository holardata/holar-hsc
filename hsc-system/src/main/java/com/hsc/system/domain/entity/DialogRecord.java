package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通话逐句对话记录表(dialog_record)
 *
 * <p>消费 ai-callbot 旁路 ASR / AI 回复 / 转接消息后逐句落库，按 call_id 关联 CallRecord，
 * 按 channel_type 区分坐席/客户/AI 助手。平迁自 reminder_backend DialogRecord。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("dialog_record")
public class DialogRecord extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "通话ID(关联 call_record.call_id)")
    @TableField("call_id")
    private String callId;

    @Schema(description = "发言人类型 坐席/客户/AI智能坐席")
    @TableField("channel_type")
    private String channelType;

    @Schema(description = "消息类型 ASR/ai_answer/transfer")
    @TableField("msg_type")
    private String msgType;

    @Schema(description = "对话文本")
    @TableField("dialog_txt")
    private String dialogTxt;

    @Schema(description = "ASR原始JSON")
    @TableField("asr_json")
    private String asrJson;

    @Schema(description = "翻译文本")
    @TableField("trans_txt")
    private String transTxt;

    @Schema(description = "AI改写文本")
    @TableField("gaixie_txt")
    private String gaixieTxt;

    @Schema(description = "序号")
    @TableField("seq")
    private Long seq;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "说话时间")
    @TableField("speak_time")
    private Date speakTime;

    @Schema(description = "摘要处理状态 0-未处理 1-已处理")
    @TableField("status")
    private Integer status;

    @Schema(description = "音频起始时间戳(毫秒) 供前端音频-文字联动")
    @TableField("audio_start")
    private Long audioStart;

    @Schema(description = "音频结束时间戳(毫秒) 供前端音频-文字联动")
    @TableField("audio_end")
    private Long audioEnd;

    @Schema(description = "改写显示模式 0=不显示 1=仅改写 2=原文+改写")
    @TableField("gaixie_whether")
    private Integer gaixieWhether;

    @Schema(description = "来源 ai-ai-callbot对话段 / bypass-人工通话旁路ASR / transfer-转人工锚点 / recommend-人工坐席推荐话术")
    @TableField("source")
    private String source;
}
