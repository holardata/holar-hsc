package com.hsc.common.config.redis;

import cn.hutool.core.util.StrUtil;
import com.hsc.common.event.RedisQueueMessageEvent;
import com.hsc.common.utils.VendorParamsCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 队列消息消费者：守护线程循环 BLPOP，解析消息头参数后发 {@link RedisQueueMessageEvent}，
 * 由业务层异步监听处理。
 *
 * <p>仅被 hsc-api（{@code @ComponentScan("com.hsc")}）装配；hsc-file-client 用 {@code @SpringBootApplication}
 * 只扫自身包，不会启动本消费者，天然避免多进程重复消费。
 *
 * <p>平迁自 reminder_backend RedisMessageProcessor（BLPOP + 异步分发范式），适配 hsc 的
 * ApplicationEvent 机制与优雅停机（@PreDestroy 中断守护线程）。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisMessageProcessor {

    private final RedisMessageQueueService redisMessageQueueService;
    private final ApplicationEventPublisher eventPublisher;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread processorThread;

    @PostConstruct
    public void init() {
        running.set(true);
        processorThread = new Thread(this::processMessagesLoop, "Redis-Message-Processor");
        processorThread.setDaemon(true);
        processorThread.start();
        log.info("Redis 消息消费者启动，监听队列: {}", RedisMessageQueueService.WEBSOCKET_MESSAGE_QUEUE);
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        if (processorThread != null) {
            processorThread.interrupt();
        }
        log.info("Redis 消息消费者停止");
    }

    private void processMessagesLoop() {
        // 启动延迟，等容器与依赖就绪
        sleepQuietly(5000);
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            String message;
            try {
                message = redisMessageQueueService.receiveMessageBlocking(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Redis 队列读取异常", e);
                sleepQuietly(1000);
                continue;
            }
            if (message == null) {
                continue;
            }
            try {
                dispatch(message);
            } catch (Exception e) {
                log.error("Redis 消息分发异常: {}", message, e);
            }
        }
    }

    /** 解析消息（encodedParams|msgBody）并发布事件供业务层监听。 */
    private void dispatch(String message) {
        int idx = message.indexOf(VendorParamsCodec.MSG_SEPARATOR);
        String encodedParams;
        String msgBody;
        if (idx < 0) {
            // 无分隔符，整体当消息体（容错）
            encodedParams = "";
            msgBody = message;
        } else {
            encodedParams = message.substring(0, idx);
            msgBody = message.substring(idx + 1);
        }
        Map<String, String> params = StrUtil.isBlank(encodedParams)
                ? Collections.emptyMap()
                : VendorParamsCodec.parseParams(encodedParams);
        eventPublisher.publishEvent(new RedisQueueMessageEvent(this, params, msgBody));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
