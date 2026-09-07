package com.hsc.common.annotation;


import java.lang.annotation.*;

/**
 * TTS 引擎类型注解（unify-voice-engine-config）。
 *
 * <p>标注在 {@code TtsEngineAdapter} 实现类上，value 为 {@code VoiceEngineTypeEnum#getCode()}
 * （如 "aliyun-tts"/"volc-tts"），由 {@code TtsEngineAdapterFactory} 构造时扫描收集、
 * 运行时按引擎类型路由分发——同 {@code @EslRouteName} + {@code FsEslRouteFactory} 的
 * 「注解驱动 + Map 注入 Factory」范式。新增引擎类型只需加一个带注解的适配器组件。
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TtsEngineType {

    /** 引擎类型标识（对齐 VoiceEngineTypeEnum.code） */
    String value();
}
