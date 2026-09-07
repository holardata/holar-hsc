package com.hsc.api.controller.sys;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.query.brand.BrandConfigQuery;
import com.hsc.system.domain.vo.brand.BrandConfigVo;
import com.hsc.system.service.IBrandConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 品牌设置(site_config)管理。OEM 品牌要素（应用名/logo/favicon/登录页文案与大图）。
 *
 * <p>{@code /get} 与 {@code /boot.js}（首屏同步引导脚本）免鉴权（登录页登录前就要展示
 * 品牌，SecurityConfig 白名单放行）；保存需登录 + @PreAuthorize（sys_menu F 按钮
 * system:brand:save）。
 * 图片上传复用 {@link SysFileController} 的通用上传，URL 走 /system/v1/file/play/{id}（免鉴权）。
 */
@Tag(name = "品牌设置")
@RestController
@RequestMapping("/system/v1/brandConfig")
public class BrandConfigController extends BaseController {

    @Autowired
    private IBrandConfigService brandConfigService;

    @Operation(summary = "获取品牌配置(登录页免鉴权)", method = "GET")
    @GetMapping("/get")
    public ResResult<BrandConfigVo> get() {
        // 免鉴权：登录页展示品牌，白名单放行 /system/v1/brandConfig/get
        return success(brandConfigService.getBrandConfig());
    }

    /** boot.js 序列化专用（ appName 单字符串，无配置特性需求，不必注入 Spring 容器） */
    private static final ObjectMapper BOOT_JS_MAPPER = new ObjectMapper();

    /**
     * OEM 品牌首屏引导脚本：前端 index.html head 最前同步（阻塞渲染直至返回）加载，
     * 使标签页标题与首屏渲染在显示前就拿到数据库当前品牌配置（全量 BrandConfigVo）——
     * 前端 brandStore 以它预填初始值，登录页应用名/logo/文案/大图首渲染即 OEM 值，
     * 不闪构建默认名/旧值。免鉴权同 /get；no-store 是硬约束（被缓存会拿到旧值，
     * 重新引入闪变）。
     */
    @Operation(summary = "品牌首屏引导脚本(免鉴权)", method = "GET")
    @GetMapping(value = "/boot.js", produces = "text/javascript;charset=UTF-8")
    public String bootJs(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try {
            return "window.__HSC_BOOT__ = "
                    + BOOT_JS_MAPPER.writeValueAsString(brandConfigService.getBrandConfig()) + ";";
        } catch (JsonProcessingException e) {
            // 序列化异常兜底空配置 = 前端回退构建默认品牌
            return "window.__HSC_BOOT__ = {};";
        }
    }

    @Log(title = "保存品牌配置", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:brand:save')")
    @Operation(summary = "保存品牌配置(整段,null/空=回退默认)", method = "POST")
    @PostMapping("/save")
    public ResResult save(@RequestBody BrandConfigQuery query) {
        // 全部字段传空保存 = 一键恢复默认品牌
        brandConfigService.saveBrandConfig(query);
        return success();
    }
}
