package com.hsc.common.config.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列服务：阻塞式(BLPOP)消费 {@code websocket:message:queue}。
 *
 * <p>用 {@link StringRedisTemplate}（raw string）消费 ai-callbot 推送的原始字符串消息，
 * 不走 {@link RedisService} 的 fastjson 序列化 RedisTemplate——ai-callbot 推送的是
 * {@code encodedParams|asrJson} 文本（非合法 JSON），fastjson 反序列化会失败。
 * 平迁自 reminder_backend RedisMessageQueueService。
 */
@Component
public class RedisMessageQueueService {

    /** ai-callbot 与 hsc 共用的通话消息队列（与 ai-callbot redis_publisher.QUEUE_KEY 一致）。 */
    public static final String WEBSOCKET_MESSAGE_QUEUE = "websocket:message:queue";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 阻塞式读取队列(BLPOP)，最多等待 timeout；超时无消息返回 null。
     */
    public String receiveMessageBlocking(long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForList().leftPop(WEBSOCKET_MESSAGE_QUEUE, timeout, unit);
    }
}
