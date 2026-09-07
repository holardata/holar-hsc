package com.hsc.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hsc.ai.constant.PromptTitle;
import com.hsc.ai.service.ISummaryService;
import com.hsc.common.event.CallHangupEvent;
import com.hsc.common.thread.ThreadFactoryImpl;
import com.hsc.system.domain.entity.CallAiSummary;
import com.hsc.system.service.ICallAiSummaryService;
import com.hsc.system.service.IDialogRecordService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通话 AI 摘要生成监听器（call-ai-summary 挂断全文摘要，1.12 CallHangupEvent 的消费者）。
 *
 * <p>通话最后一路挂断时触发：拼该 callId 的全部 ASR 句子为对话文本，经 {@link ISummaryService}（hsc-ai 统一网关）
 * 生成全文总结/关键词/角色总结/要点/代办/思维导图 6 个字段，写入 call_ai_summary。
 *
 * <p>异步执行(@Async)，挂断摘要不阻塞 ESL 事件线程。摘要调用失败时 ISummaryService 返回占位文案，不中断。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CallAiSummaryListener {

    private final IDialogRecordService dialogRecordService;
    private final ICallAiSummaryService callAiSummaryService;
    private final ISummaryService summaryService;

    /** 摘要生成专用线程池：6 字段并行，与 asyncTaskExecutor 隔离，避免慢 LLM 拖垮转写/推送 */
    private final ExecutorService summaryExecutor = new ThreadPoolExecutor(
            6, 6, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryImpl("call-summary-"));

    @Async("asyncTaskExecutor")
    @EventListener
    public void onHangup(CallHangupEvent event) {
        String callId = event.getCallId();
        String dialogText = dialogRecordService.buildAsrDialogText(callId);
        if (StrUtil.isBlank(dialogText)) {
            log.info("挂断全文摘要跳过：无 ASR 记录 callId={}", callId);
            return;
        }
        log.info("生成挂断全文摘要 callId={}", callId);

        // 6 个字段互相独立，并行生成(专用线程池)：把 6×串行 降到 ≈1×单次，
        // 同时避免长时间占用 asyncTaskExecutor 拖垮转写落库/WS 推送等共用任务
        CompletableFuture<String> overall = generateAsync(callId, PromptTitle.OVERALL_SUMMARY, dialogText);
        CompletableFuture<String> keyword = generateAsync(callId, PromptTitle.KEYWORD_ABSTRACT, dialogText);
        // 角色总结走 generateRoleSummary(规整 LLM 偶发缺 [ ] 的输出)，前端按 JSON 数组渲染角色卡片
        CompletableFuture<String> role = CompletableFuture.supplyAsync(
                () -> summaryService.generateRoleSummary(callId, dialogText), summaryExecutor);
        CompletableFuture<String> keypoint = generateAsync(callId, PromptTitle.KEYPOINT_EXTRACT, dialogText);
        CompletableFuture<String> todo = generateAsync(callId, PromptTitle.TODO_THINGS, dialogText);
        // 脑图走 generateXmindJson(剥离 ```json 代码块 + FIX_JSON修正 + 兜底)：LLM 脑图输出常带
        // 代码块包裹，直接存原始输出会致前端 JSON.parse 失败。仍放 summaryExecutor 与其他 5 字段并行。
        CompletableFuture<String> xmind = CompletableFuture.supplyAsync(
                () -> summaryService.generateXmindJson(callId, dialogText), summaryExecutor);
        CompletableFuture.allOf(overall, keyword, role, keypoint, todo, xmind).join();

        CallAiSummary summary = callAiSummaryService.getOrCreateByCallId(callId);
        summary.setSummary(overall.join());
        summary.setSummaryStatus(2);
        summary.setKeywordAbstract(keyword.join());
        summary.setKeywordStatus(2);
        summary.setRoleSummary(role.join());
        summary.setRoleStatus(2);
        summary.setKeypointExtract(keypoint.join());
        summary.setKeypointStatus(2);
        summary.setTodoThings(todo.join());
        summary.setTodoStatus(2);
        summary.setMarkdownJson(xmind.join());
        summary.setXmindStatus(2);
        callAiSummaryService.saveOrUpdate(summary);
        log.info("挂断全文摘要完成 callId={}", callId);
    }

    /** 在 summaryExecutor 上异步生成单个摘要字段(失败返回占位，不抛)。 */
    private CompletableFuture<String> generateAsync(String callId, String title, String dialogText) {
        return CompletableFuture.supplyAsync(
                () -> safe(summaryService.generate(callId, title, dialogText)), summaryExecutor);
    }

    @PreDestroy
    public void destroy() {
        summaryExecutor.shutdown();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
