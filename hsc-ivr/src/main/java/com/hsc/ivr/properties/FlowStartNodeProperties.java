// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开始节点属性参数
 * @author: danmo
 * @date 2024/12/29 19:05
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowStartNodeProperties extends FlowNodeProperties{


    // 是否录音
    private Boolean recording;

    // ASR 引擎实例 id（voice_engine，unify-voice-engine-config；供后续语音识别类节点用）
    private Long asrEngine;

    // TTS 引擎实例 id（voice_engine；文本放音预合成用）
    private Long ttsEngine;


}
