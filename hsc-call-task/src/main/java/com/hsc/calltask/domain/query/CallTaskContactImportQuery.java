// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 呼叫任务联系人导入参数
 * @author danmo
 * @date 2025/06/19 10:10
 */
@Schema(description = "呼叫任务联系人导入参数")
@Data
public class CallTaskContactImportQuery  {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "导入方式 0-人群导入 1-文件导入")
    private Integer importType;

    @Schema(description = "人群ID")
    private Long crowdId;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "冲突强制导入：首次导入返回 conflict=true（跨进行中任务重复/号码已归属私海），"
            + "前端确认后带 true 重调放行")
    private Boolean force;

    @Schema(description = "Excel 未匹配号码同步创建公海档案（默认 false 通知型快车道；true 时按导入模板建档,来源=任务导入）")
    private Boolean createCustomer;
}
