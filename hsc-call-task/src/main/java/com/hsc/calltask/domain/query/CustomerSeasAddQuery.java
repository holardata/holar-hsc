// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2025/06/27 09:07
 */
@Schema(description = "客户公海新增参数")
@Data
public class CustomerSeasAddQuery {

    @Schema(description = "主键ID", hidden = true)
    private Long id;


    @Schema(description = "模板ID")
    private Long templateId;


    @Schema(description = "客户数据")
    private String customerInfo;
}
