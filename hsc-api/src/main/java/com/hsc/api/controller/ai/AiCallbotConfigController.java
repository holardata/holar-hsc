package com.hsc.api.controller.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.AiCallbotConfig;
import com.hsc.system.service.IAiCallbotConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 坐席配置管理（ai_callbot_config，单例 id=1）。平迁自 reminder_backend AiCallbotConfigController。
 *
 * <p>供配置页维护 + ai-callbot 每通电话 HTTP 拉取。{@code /get} 免鉴权（ai-callbot 内部拉取，
 * 由 SecurityConfig 白名单放行）；其余接口需登录 + @PreAuthorize。
 */
@Tag(name = "AI坐席配置")
@RestController
@RequestMapping("/system/v1/aiCallbotConfig")
public class AiCallbotConfigController extends BaseController {

    @Autowired
    private IAiCallbotConfigService iAiCallbotConfigService;

    @Operation(summary = "获取AI坐席配置(供ai-callbot拉取,免鉴权)", method = "GET")
    @GetMapping("/get")
    public ResResult<JSONObject> get() {
        // 免鉴权：ai-callbot 内部拉取，需在 SecurityConfig 白名单放行 /system/v1/aiCallbotConfig/get
        // unify-voice-engine-config：引擎引用 id 在此读时展开为 RuntimeConfig 平铺字段（形状不变，
        // Python 侧零改动）；无引用 id（未配置引擎）时原样返回业务字段。
        JSONObject body = new JSONObject();
        body.put("config", iAiCallbotConfigService.getExpandedConfig());
        return success(body);
    }

    @Log(title = "保存AI坐席配置", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:aiCallbotConfig:edit')")
    @Operation(summary = "保存AI坐席配置(单例)", method = "POST")
    @PostMapping("/save")
    public ResResult save(@RequestBody JSONObject body) {
        // 前端提交 {config:{...}}（与 get 返回 {config:{...}} 对称）；存为 configJson 整段。
        // WriteMapNullValue：保留引擎引用 id 的显式 null（前端清空下拉=解除引用，Service 据此 remove key）
        Object configObj = body != null ? body.get("config") : null;
        AiCallbotConfig config = new AiCallbotConfig();
        config.setConfigJson(configObj == null ? "{}"
                : JSON.toJSONString(configObj, JSONWriter.Feature.WriteMapNullValue));
        iAiCallbotConfigService.saveOrUpdateSingle(config);
        return success();
    }

    @Log(title = "AI坐席知识库列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:aiCallbotConfig:get')")
    @Operation(summary = "holargpt知识库列表(配置页下拉)", method = "GET")
    @GetMapping("/datasets")
    public ResResult<Object> datasets() {
        return success(iAiCallbotConfigService.listDatasets());
    }

    @Log(title = "AI坐席智能体列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:aiCallbotConfig:get')")
    @Operation(summary = "holargpt智能体应用列表(配置页下拉)", method = "GET")
    @GetMapping("/apps")
    public ResResult<Object> apps() {
        return success(iAiCallbotConfigService.listApps());
    }

    // unify-voice-engine-config：test/asr、test/tts 迁至 VoiceEngineController（/call/v1/voiceEngine/test/*，
    // 按引擎实例 id 测试，登录即可访问）——引擎参数已外迁 voice_engine 表，本控制器不再持有引擎参数。
}
