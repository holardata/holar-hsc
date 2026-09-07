package com.hsc.api.controller.sys;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.query.config.SysConfigEditQuery;
import com.hsc.system.domain.query.config.SysConfigQuery;
import com.hsc.system.domain.vo.config.SysConfigVo;
import com.hsc.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统参数管理：参数由系统预置不可增删(无 add/delete 接口)，
 * 仅支持分页查询与"参数值/备注"修改(保存即刷新缓存实时生效)。
 *
 * @author pangshuai
 * @date 2026-09-01
 */
@Tag(name = "系统参数管理")
@RestController
@RequestMapping("/system/v1/sysConfig")
public class SysConfigController extends BaseController {

    @Autowired
    private ISysConfigService iSysConfigService;

    @Log(title = "参数列表(分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:sysConfig:list')")
    @Operation(summary = "参数列表(分页)", method = "POST")
    @PostMapping("/list")
    public ResResult<PageInfo<SysConfigVo>> list(@RequestBody SysConfigQuery query) {
        return success(iSysConfigService.pageList(query));
    }

    @Log(title = "修改参数值/备注", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:sysConfig:edit')")
    @Operation(summary = "修改参数值/备注", method = "POST")
    @PostMapping("/edit")
    public ResResult edit(@RequestBody @Validated SysConfigEditQuery query) {
        iSysConfigService.edit(query);
        return success();
    }
}
