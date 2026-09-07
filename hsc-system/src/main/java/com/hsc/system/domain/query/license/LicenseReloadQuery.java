package com.hsc.system.domain.query.license;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 导入许可证入参
 *
 * @date 2026-08-28
 */
@Data
public class LicenseReloadQuery {

    @NotBlank(message = "许可证不能为空")
    private String licenseCode;
}
