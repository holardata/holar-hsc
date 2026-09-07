package com.hsc.api.controller.call;

import com.hsc.api.service.IExportService;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.query.dialogue.DialogueExportQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通话导出（平迁自 reminder_backend ExportController）。
 *
 * <p>按 callId 导出原文/AI改写/摘要/录音，打包 zip 下载。
 * 下载为二进制流，直接写 HttpServletResponse（文件流场景，非 JSON 业务接口，故不走 ResResult）。
 */
@Tag(name = "通话导出")
@RestController
@RequestMapping("/system/v1/export")
public class ExportController extends BaseController {

    @Autowired
    private IExportService iExportService;

    @Log(title = "导出通话记录", businessType = BusinessTypeEnum.EXPORT)
    @PreAuthorize("@authz.hasPerm('system:dialogue:export')")
    @Operation(summary = "按callId导出通话(zip含原文/AI改写/摘要/录音)", method = "POST")
    @PostMapping("/exportCallRecord")
    public void exportCallRecord(@RequestBody DialogueExportQuery query, HttpServletResponse response) {
        iExportService.exportCallRecord(query, response);
    }
}
