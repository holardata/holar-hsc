// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.display;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author danmo
 * @date 2023-10-23 11:20
 **/
@Schema
@Data
public class CallDisplayAddQuery {

    /**
     * 主键
     */
    @Schema(description = "主键",hidden = true)
    private Long id;


    /**
     *  电话号码
     */
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^(1[3-9]\\d{9}|\\d{3,4}-?\\d{7,8}|[48]00\\d{7})$",message = "电话号码格式不正确")
    @Schema(description = "电话号码",requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
}
