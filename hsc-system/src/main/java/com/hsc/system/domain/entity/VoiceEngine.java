package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 语音引擎实例表(VoiceEngine)表实体类（unify-voice-engine-config）。
 * 一行 = 一个引擎实例（同类型可多实例：多账号/多音色/多环境）；
 * ASR/TTS 连接参数统一在此维护，AI 坐席配置 / IVR / 语音文件合成引用本表 id。
 * 替代旧 call_engine（MRCP 遗产，链路不可用，代码已下线、表保留）。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("voice_engine")
public class VoiceEngine extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 引擎实例名称（如 阿里TTS-甜美女声）
     */
    @Schema(description = "引擎实例名称")
    @TableField("name")
    private String name;

    /**
     * 引擎类型 aliyun-tts/pro/openai-tts/holartts/volc-tts/funasr/aliyun-nls/tencent-asr/xfyun-asr
     */
    @Schema(description = "引擎类型(见 VoiceEngineTypeEnum)")
    @TableField("engine_type")
    private String engineType;

    /**
     * 该类型连接参数 JSON（key 与 ai-callbot RuntimeConfig 平铺字段名一致）
     */
    @Schema(description = "连接参数JSON(key与RuntimeConfig平铺字段名一致)")
    @TableField("config")
    private String config;

    /**
     * 备注
     */
    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

}
