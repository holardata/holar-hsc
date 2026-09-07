package com.hsc.system.domain.query.dialogue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通话导出入参（平迁自 reminder_backend HistoryRecordExport）。
 *
 * <p>各文档 format 取值 docx/pdf/txt（空表示不导出该类）；showInfo 含"发言人"/"时间戳"控制每行附加信息。
 * 按 callId 导出原文/AI改写/摘要/录音 打包 zip。
 */
@Schema
@Data
public class DialogueExportQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通话ID")
    private String callId;

    @Schema(description = "原文文档格式 docx/pdf/txt(空=不导出)")
    private String dialogTxtFormat;

    @Schema(description = "原文显示信息 含'发言人'/'时间戳'")
    private String dialogTxtShowInfo;

    @Schema(description = "AI改写内容 改写结果/原文与改写对照")
    private String gaixieContent;

    @Schema(description = "AI改写文档格式 docx/pdf/txt")
    private String gaixieFormat;

    @Schema(description = "AI改写显示信息 含'发言人'/'时间戳'")
    private String gaixieShowInfo;

    @Schema(description = "摘要文档格式 docx/pdf/txt")
    private String summaryFormat;

    @Schema(description = "摘要内容 含'关键词/全文总结/过程摘要/角色总结/要点提炼/代办事项'")
    private String summaryAll;

    @Schema(description = "录音文件格式 wav(空=不导出)")
    private String audioFileFormat;
}
