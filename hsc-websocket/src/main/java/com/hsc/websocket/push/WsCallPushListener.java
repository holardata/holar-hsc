package com.hsc.websocket.push;

import com.hsc.common.event.AbstractUpdatedEvent;
import com.hsc.common.event.AgentRecommendEvent;
import com.hsc.common.event.CallStatusEvent;
import com.hsc.common.event.DialogUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 通话实时数据推送监听器：把 hsc-system 发布的通话事件桥接到 {@link WsCallPushService} 推送给坐席前端。
 *
 * <p>异步执行(@Async)，避免阻塞 ApplicationEventPublisher 与 Redis 消费线程。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WsCallPushListener {

    private final WsCallPushService wsCallPushService;

    @Async("asyncTaskExecutor")
    @EventListener
    public void onDialog(DialogUpdatedEvent event) {
        wsCallPushService.pushRealtimeDialog(event.getCallId(), event.getChannelType(),
                event.getDialogTxt(), event.getMode(), event.getCreateTime(), event.getCallerNumber());
    }

    @Async("asyncTaskExecutor")
    @EventListener
    public void onCallStatus(CallStatusEvent event) {
        wsCallPushService.pushCallStatus(event.getAgentNumber(), event.getCallId(),
                event.getStatusType(), event.getCallerNumber());
    }

    @Async("asyncTaskExecutor")
    @EventListener
    public void onAbstract(AbstractUpdatedEvent event) {
        wsCallPushService.pushAbstract(event.getAgentNumber(), event.getCallId(),
                event.getTitle(), event.getContent());
    }

    @Async("asyncTaskExecutor")
    @EventListener
    public void onAgentRecommend(AgentRecommendEvent event) {
        wsCallPushService.pushAgentRecommend(event.getCallId(), event.getRecommendTxt(), event.isFinal());
    }
}
