package com.hsc.api.controller.ai;

import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.HumanAgentConfig;
import com.hsc.system.service.IHumanAgentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人工坐席助手配置管理（平迁自 reminder_backend HumanAgentConfigController）。
 *
 * <p>单例配置(id=1)：holargpt 连接配置 + 选用智能体应用。运行时由 holargpt 推荐话术链路读取。
 * 适配 hsc 结构化字段模型（rb 用 configJson 整串，hsc 1.19 已拆为结构化字段）。
 */
@Tag(name = "人工坐席助手配置")
@RestController
@RequestMapping("/system/v1/humanAgentConfig")
public class HumanAgentConfigController extends BaseController {

    @Autowired
    private IHumanAgentConfigService iHumanAgentConfigService;

    @Log(title = "获取坐席助手配置", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:humanAgentConfig:get')")
    @Operation(summary = "获取人工坐席助手配置(单例)", method = "GET")
    @GetMapping("/get")
    public ResResult<HumanAgentConfig> get() {
        return success(iHumanAgentConfigService.getSingle());
    }

    @Log(title = "保存坐席助手配置", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:humanAgentConfig:edit')")
    @Operation(summary = "保存人工坐席助手配置(单例)", method = "POST")
    @PostMapping("/save")
    public ResResult save(@RequestBody HumanAgentConfig config) {
        iHumanAgentConfigService.saveOrUpdateSingle(config);
        return success();
    }

    @Log(title = "智能体应用列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:humanAgentConfig:get')")
    @Operation(summary = "holargpt智能体应用列表(配置页下拉)", method = "GET")
    @GetMapping("/apps")
    public ResResult<Object> apps() {
        return success(iHumanAgentConfigService.listApps());
    }
}
