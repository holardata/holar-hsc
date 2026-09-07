package com.hsc.system.util.tts;

import com.hsc.common.annotation.TtsEngineType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TTS 引擎适配器工厂（unify-voice-engine-config）。
 *
 * <p>「注解驱动 + Map 注入 Factory」范式（同 FsEslRouteFactory）：构造注入所有
 * {@code TtsEngineAdapter} Bean，按实现类上的 {@code @TtsEngineType} 注解 value
 * （对齐 VoiceEngineTypeEnum.code）建立类型 → 适配器路由。新增引擎类型加一个带注解的
 * 适配器组件即可，无需改本工厂。
 */
@Slf4j
@Component
public class TtsEngineAdapterFactory {

    private final Map<String, TtsEngineAdapter> adapterMap = new ConcurrentHashMap<>(8);

    public TtsEngineAdapterFactory(Map<String, TtsEngineAdapter> adapterMap) {
        // Spring 按 Bean 名注入，此处改为按 @TtsEngineType 注解 value 索引
        for (TtsEngineAdapter adapter : adapterMap.values()) {
            TtsEngineType annotation = adapter.getClass().getAnnotation(TtsEngineType.class);
            if (annotation == null) {
                annotation = adapter.getClass().getSuperclass().getAnnotation(TtsEngineType.class);
            }
            if (annotation == null) {
                log.warn("TTS 适配器缺少 @TtsEngineType 注解，跳过: {}", adapter.getClass().getName());
                continue;
            }
            this.adapterMap.put(annotation.value(), adapter);
        }
    }

    /**
     * 按引擎类型取适配器合成。
     *
     * @param engineType 引擎类型标识（VoiceEngineTypeEnum.code）
     * @param config     引擎连接参数
     * @param text       合成文本
     */
    public TtsResult synthesize(String engineType, com.alibaba.fastjson2.JSONObject config, String text) {
        TtsEngineAdapter adapter = adapterMap.get(engineType);
        if (Objects.isNull(adapter)) {
            throw new RuntimeException("暂不支持的 TTS 引擎类型: " + engineType);
        }
        return adapter.synthesize(config, text);
    }
}
