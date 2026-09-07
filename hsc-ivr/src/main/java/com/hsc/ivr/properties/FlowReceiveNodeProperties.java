package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowReceiveNodeProperties extends FlowNodeProperties {

    /**
     * 播放类型 1-文件 2-内容
     */
    private Integer playbackType;

    /**
     * 播放文件
     */
    private Long fileId;

    /**
     * 播放内容（支持 ${变量} 混播）
     */
    private String content;

    /**
     * 未按键播放类型 1-文件 2-内容
     */
    private Integer notPlaybackType;

    /**
     * 未按键播放文件
     */
    private Long notFileId;

    /**
     * 未按键播放内容
     */
    private String notContent;

    /**
     * 错按键播放类型 1-文件 2-内容
     */
    private Integer errorPlaybackType;

    /**
     * 错按键播放文件
     */
    private Long errorFileId;

    /**
     * 错按键播放内容
     */
    private String errorContent;

    /**
     * 号码最大位数
     */
    private Integer numMax;

    /**
     * 号码最小位数
     */
    private Integer numMin;

    /**
     * 结束按键（如 #）
     */
    private String endPoint;

    /**
     * 按键超时时间(ms)
     */
    private Integer timeout;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 收号结果存入变量袋的键名（缺省按节点 id）
     */
    private String varName;
}
