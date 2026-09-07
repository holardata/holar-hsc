// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;


import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户群客户查询参数
 * @author danmo
 * @date 2025/7/4 9:53
 */
@Schema(description = "客户群客户查询参数")
@EqualsAndHashCode(callSuper = true)
@Data
public class CrowdCustomerQuery extends BaseQuery {

    @NotNull(message = "人群ID不能为空")
    @Schema(description = "人群ID")
    private Long crowdId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户电话")
    private String customerPhone;

    @Schema(description = "客户来源 0-手动创建 1-文件导入 2-API导入")
    private Integer customerSource;
}
