// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowTransferNodeProperties extends FlowNodeProperties{

    /**
     * 路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-转IVR 7-座机 8-智能坐席（与号码路由一致）
     */
    private Integer routeType;

    /**
     * 路由类型值
     */
    private String routeValue;
}
