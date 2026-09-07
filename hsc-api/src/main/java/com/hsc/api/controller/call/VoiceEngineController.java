package com.hsc.api.controller.call;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.VoiceEngine;
import com.hsc.system.domain.enums.VoiceEngineTypeEnum;
import com.hsc.system.domain.query.engine.VoiceEngineAddQuery;
import com.hsc.system.domain.query.engine.VoiceEngineQuery;
import com.hsc.system.service.IVoiceEngineService;
import com.hsc.system.util.tts.TtsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 语音引擎实例管理（voice_engine，unify-voice-engine-config）。
 * 替代旧 CallEngineController（call_engine MRCP 遗产，已下线）。
 * ASR/TTS 引擎一处配置，AI智能坐席配置 / IVR / 语音文件合成引用本表实例。
 */
@Tag(name = "语音引擎管理")
@RestController
@RequestMapping("/call/v1/voiceEngine")
public class VoiceEngineController extends BaseController {

    @Autowired
    private IVoiceEngineService iVoiceEngineService;

    @Operation(summary = "引擎类型清单(前端类型下拉/参数表单schema)", method = "GET")
    @GetMapping("/types")
    public ResResult<List<JSONObject>> types() {
        // 类型=代码内置适配器清单（VoiceEngineTypeEnum），新增类型=加适配器+schema，非用户自定义
        return success(Arrays.stream(VoiceEngineTypeEnum.values())
                .map(t -> {
                    JSONObject o = new JSONObject();
                    o.put("code", t.getCode());
                    o.put("label", t.getLabel());
                    o.put("kind", t.getKind());
                    o.put("flatFields", t.getFlatFields());
                    return o;
                }).toList());
    }

    @Log(title = "新增语音引擎", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('call:voiceEngine:add')")
    @Operation(summary = "新增语音引擎", method = "POST")
    @PostMapping("/add")
    public ResResult addEngine(@RequestBody @Validated VoiceEngineAddQuery query) {
        iVoiceEngineService.addEngine(query);
        return success();
    }

    @Log(title = "修改语音引擎", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:voiceEngine:edit')")
    @Operation(summary = "修改语音引擎", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated VoiceEngineAddQuery query) {
        query.setId(id);
        iVoiceEngineService.edit(query);
        return success();
    }

    @Log(title = "删除语音引擎", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('call:voiceEngine:delete')")
    @Operation(summary = "删除语音引擎(被AI配置/语音文件引用时阻止)", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody VoiceEngineQuery query) {
        iVoiceEngineService.delete(query);
        return success();
    }

    @Operation(summary = "分页查询语音引擎", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<VoiceEngine>> pageList(@RequestBody VoiceEngineQuery query) {
        // PageInfo 包装对齐前端契约（vxe 读 {list,total}）；直接返回 List 会被序列化成裸数组导致前端列表空
        PageInfo<VoiceEngine> pageInfo = new PageInfo<>(iVoiceEngineService.getPageList(query));
        return success(pageInfo);
    }

    @Operation(summary = "查询语音引擎列表(kind=asr/tts便捷过滤,配置页下拉用)", method = "POST")
    @PostMapping("/list")
    public ResResult<List<VoiceEngine>> list(@RequestBody VoiceEngineQuery query) {
        return success(iVoiceEngineService.getList(query));
    }

    @Operation(summary = "查询语音引擎详情", method = "GET")
    @GetMapping("/get/{id}")
    public ResResult<VoiceEngine> get(@PathVariable("id") Long id) {
        return success(iVoiceEngineService.getDetail(id));
    }

    // ---- 测试接口（原 AiCallbotConfigController 的 test/asr、test/tts 迁入；登录即可访问） ----

    @Log(title = "ASR连通性测试", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "ASR连通性测试(按引擎实例,返回响应延迟ms)", method = "POST")
    @PostMapping("/test/asr")
    public ResResult<Long> testAsr(@RequestBody JSONObject body) {
        Long engineId = body != null ? body.getLong("engineId") : null;
        return success(iVoiceEngineService.testAsr(engineId));
    }

    @Log(title = "TTS试听", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "TTS试听(按引擎实例当前参数合成返回音频流)", method = "POST")
    @PostMapping("/test/tts")
    public void testTts(@RequestBody JSONObject body, HttpServletResponse response) throws IOException {
        Long engineId = body != null ? body.getLong("engineId") : null;
        String text = body != null ? StrUtil.nullToEmpty(body.getString("text")).trim() : "";
        if (text.isEmpty()) {
            text = "您好，这是一段测试语音。";
        }
        try {
            TtsResult r = iVoiceEngineService.synthesizeByEngineId(engineId, text);
            response.setContentType(r.getContentType());
            response.getOutputStream().write(r.getBody());
            response.getOutputStream().flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            JSONObject err = new JSONObject();
            err.put("code", 500);
            err.put("msg", e.getMessage() != null ? e.getMessage() : "TTS 失败");
            response.getOutputStream().write(err.toJSONString().getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        }
    }
}
