// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.subsriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author danmo
 * @date 2023年09月25日 13:58
 */
@Schema
@Data
public class KoSubscriberAddQuery {

    @NotEmpty(message = "账号不能为空")
    @Schema(description = "username",requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;


    @Schema(description = "域")
    private String domain;

    @Schema(description = "password(座机必填纯数字≥8;软电话留空后端自动生成)")
    private String password;

    @Schema(description = "vmpin")
    private String vmpin;

    @Schema(description = "ha1")
    private String ha1;

    @Schema(description = "ha1b")
    private String ha1b;

    @Schema(description = "状态 0-开启 1-关闭")
    private Integer status;

    @Schema(description = "终端类型 0-软电话 1-座机")
    private Integer terminalType;
}
