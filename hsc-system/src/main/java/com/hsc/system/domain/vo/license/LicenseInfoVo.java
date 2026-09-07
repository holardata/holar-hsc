package com.hsc.system.domain.vo.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 许可证授权状态
 *
 * @date 2026-08-28
 */
@Schema
@Builder
@Data
public class LicenseInfoVo {

    @Schema(description = "是否已授权本产品（additional 含 hsc 且未过期）")
    private Boolean authorized;

    @Schema(description = "到期时间（毫秒时间戳，未授权时可能为空）")
    private Long expireTime;

    @Schema(description = "未授权原因提示（已授权为空）")
    private String reason;
}
