package com.hsc.system.util.tts;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * TTS 合成结果（音频字节 + Content-Type）。
 * 从 IAiCallbotConfigService.TtsResult 内部类平移而来（合成能力外迁到引擎适配器层，原内部类删除）。
 */
@Data
@AllArgsConstructor
public class TtsResult {

    /** 音频内容 */
    private byte[] body;

    /** MIME 类型（audio/wav、audio/mpeg 等） */
    private String contentType;
}
