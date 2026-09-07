// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author danmo
 * @date 2023-11-01 16:13
 **/
@Schema
@Data
public class VoiceFileAddQuery {

    /**
     *  主键ID
     */

    @Schema(description = "主键ID",hidden = true)
    private Long id;
    /**
     *  文件名称
     */
    @Schema(description = "文件名称",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件名称不能为空")
    private String name;

    /**
     *  1-手动上传 2-语音合成
     */
    @NotNull(message = "类型不能为空")
    @Schema(description = "类型 1-手动上传 2-语音合成",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;



    /**
     *  TTS 引擎 aliyun-阿里云NLS / pro-专业版IndexTTS / holartts-本地HolarTTS / local-本地Qwen-TTS（type=2 生效）
     */
    @Schema(description = "TTS引擎 aliyun/pro/holartts/local(type=2生效)")
    private String tts;


    /**
     *  合成文本
     */
    @Schema(description = "合成文本")
    private String speechText;


    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private Long fileId;

}
