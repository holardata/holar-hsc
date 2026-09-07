package com.hsc.api.service;

import com.hsc.system.domain.query.dialogue.DialogueExportQuery;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 通话导出服务：按 callId 把原文/AI改写/摘要/录音打包 zip 写入 HTTP 响应下载。
 *
 * <p>平迁自 reminder_backend ExportController.exportHistoryRecord（适配 hsc CallRecord+CallAiSummary 模型）。
 * 属 web 层职责（直接操作 HttpServletResponse），故位于 hsc-api 而非 hsc-system。
 */
public interface IExportService {

    /**
     * 导出通话记录 zip。
     *
     * @param query   导出选项（callId + 各文档 format/showInfo）
     * @param response HTTP 响应（写入 zip 字节流，附件下载）
     */
    void exportCallRecord(DialogueExportQuery query, HttpServletResponse response);
}
