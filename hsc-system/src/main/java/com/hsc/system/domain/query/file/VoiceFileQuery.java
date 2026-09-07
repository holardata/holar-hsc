// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.file;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author danmo
 * @date 2023-11-01 16:13
 **/
@Schema
@Data
public class VoiceFileQuery extends BaseQuery {
    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "主键ID列表", hidden = true)
    private List<Long> ids;

    /**
     *  文件名称
     */
    @Schema(description = "文件名称")
    private String name;

    /**
     *  类型 1-本地存储 2-腾讯云 3-阿里云 9-语音合成
     */
    @NotNull(message = "类型不能为空")
    @Schema(description = "类型 1-手动上传 2-语音合成")
    private Integer type;


    /**
     * TTS 引擎 aliyun-阿里云NLS / pro-专业版IndexTTS / holartts-本地HolarTTS / local-本地Qwen-TTS（type=2 生效）
     */
    @Schema(description = "TTS引擎 aliyun/pro/holartts/local(type=2生效)")
    private String tts;

}
