// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowEndNodeProperties extends FlowNodeProperties{


    private Boolean hangUp;

    /**
     * 挂机前结束语音播放类型 1-语音文件 2-文本内容（hangUp=true 时生效）
     */
    private Integer endPlaybackType;

    /**
     * 挂机前结束语音文件ID（endPlaybackType=1）
     */
    private Long endFileId;

    /**
     * 挂机前结束语音文本，支持 ${变量} 混播（endPlaybackType=2）
     */
    private String endContent;
}
