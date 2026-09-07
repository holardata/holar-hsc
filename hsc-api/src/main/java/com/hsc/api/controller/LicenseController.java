package com.hsc.api.controller;

import com.hsc.api.service.impl.LicenseService;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.system.domain.query.license.LicenseReloadQuery;
import com.hsc.system.domain.vo.license.LicenseInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 许可证授权（licsrv 代理接口，前端不直连 licsrv）。
 * 登录前需可用，三个路径均在 SecurityConfig 免鉴权白名单。
 *
 * @date 2026-08-28
 */
@Tag(name = "许可证授权")
@RestController
@RequestMapping("/auth/v1/license")
public class LicenseController extends BaseController {

    @Autowired
    private LicenseService licenseService;

    @Operation(summary = "获取机器码", method = "GET")
    @GetMapping("/machinecode")
    public ResResult<String> machinecode() {
        return success(licenseService.getMachinecode());
    }

    @Operation(summary = "获取授权状态", method = "GET")
    @GetMapping("/info")
    public ResResult<LicenseInfoVo> info() {
        return success(licenseService.getLicenseInfo());
    }

    @Operation(summary = "导入许可证", method = "POST")
    @PostMapping("/reload")
    public ResResult<Void> reload(@Validated @RequestBody LicenseReloadQuery query) {
        licenseService.reloadLicense(query.getLicenseCode());
        return success("许可证导入成功", null);
    }
}
