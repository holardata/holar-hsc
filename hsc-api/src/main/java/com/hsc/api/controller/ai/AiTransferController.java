package com.hsc.api.controller.ai;

import com.hsc.api.service.impl.AiTransferService;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 转人工接口：ai-callbot 判断要转人工时调用，hsc 统一编排（uuid_kill ai 腿 + originate 坐席 + bridge）。
 *
 * <p>免鉴权（ai-callbot 内部调用，需在 SecurityConfig 白名单放行 /system/v1/ai/transfer）。
 * 入参 {@code {callId, trigger}}：callId 是 hsc 透传给 ai-callbot 的雪花 id（字符串），trigger 为触发方式备注。
 */
@Tag(name = "AI转人工")
@RestController
@RequestMapping("/system/v1/ai")
@RequiredArgsConstructor
public class AiTransferController extends BaseController {

    private final AiTransferService aiTransferService;

    @Operation(summary = "AI转人工(ai-callbot调用,免鉴权)", method = "POST")
    @PostMapping("/transfer")
    public ResResult transfer(@RequestBody Map<String, Object> body) {
        String callId = body != null && body.get("callId") != null ? String.valueOf(body.get("callId")) : null;
        String trigger = body != null && body.get("trigger") != null ? String.valueOf(body.get("trigger")) : "";
        boolean ok = aiTransferService.transferToHuman(callId, trigger);
        return ok ? success() : error("AI 转人工失败");
    }
}
