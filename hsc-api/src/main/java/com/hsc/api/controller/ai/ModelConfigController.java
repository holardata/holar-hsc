package com.hsc.api.controller.ai;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.ModelConfig;
import com.hsc.system.domain.query.ai.ModelConfigAddQuery;
import com.hsc.system.domain.query.ai.ModelConfigQuery;
import com.hsc.system.service.IModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * LLM 模型配置管理（model_config）：运营管理动态模型(baseUrl+model)，供摘要等业务默认调用。
 */
@Tag(name = "LLM模型配置")
@RestController
@RequestMapping("/system/v1/modelConfig")
public class ModelConfigController extends BaseController {

    @Autowired
    private IModelConfigService iModelConfigService;

    @Log(title = "模型配置列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:page:list')")
    @Operation(summary = "模型配置分页列表", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<ModelConfig>> getPageList(@RequestBody ModelConfigQuery query) {
        return success(iModelConfigService.getPageList(query));
    }

    @Log(title = "模型配置详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:get')")
    @Operation(summary = "模型配置详情", method = "GET")
    @GetMapping("/get/{id}")
    public ResResult<ModelConfig> getDetail(@PathVariable("id") Long id) {
        return success(iModelConfigService.getById(id));
    }

    @Log(title = "获取默认模型", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:get')")
    @Operation(summary = "获取默认模型", method = "GET")
    @GetMapping("/getDefault")
    public ResResult<ModelConfig> getDefault() {
        return success(iModelConfigService.getDefaultModel());
    }

    @Log(title = "新增模型配置", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:add')")
    @Operation(summary = "新增模型配置", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated ModelConfigAddQuery query) {
        iModelConfigService.add(query);
        return success();
    }

    @Log(title = "修改模型配置", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:edit')")
    @Operation(summary = "修改模型配置", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated ModelConfigAddQuery query) {
        query.setId(id);
        iModelConfigService.update(query);
        return success();
    }

    @Log(title = "删除模型配置", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:delete')")
    @Operation(summary = "删除模型配置", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody ModelConfigQuery query) {
        iModelConfigService.delete(query);
        return success();
    }

    @Log(title = "设为默认模型", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:edit')")
    @Operation(summary = "设为默认模型(其余自动置为非默认)", method = "POST")
    @PostMapping("/setDefault/{id}")
    public ResResult setDefault(@PathVariable("id") Long id) {
        iModelConfigService.setDefault(id);
        return success();
    }

    @Log(title = "启用/停用模型", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:edit')")
    @Operation(summary = "启用/停用模型", method = "POST")
    @PostMapping("/setEnabled/{id}")
    public ResResult setEnabled(@PathVariable("id") Long id, @RequestBody ModelConfigQuery query) {
        if (query.getStatus() == null) {
            throw new CommonException("状态不能为空");
        }
        iModelConfigService.setEnabled(id, query.getStatus());
        return success();
    }

    @Log(title = "模型连通性测试", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:modelConfig:get')")
    @Operation(summary = "模型连通性测试(返回响应延迟ms)", method = "POST")
    @PostMapping("/test/{id}")
    public ResResult<Long> test(@PathVariable("id") Long id) {
        return success(iModelConfigService.test(id));
    }
}
