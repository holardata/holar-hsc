// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.enums;

import lombok.Getter;

@Getter
public enum RouteTypeEnum {

    AGENT(1, "坐席"),
    CALLOUT(2, "外呼"),
    SIP(3, "sip"),
    SKILL_GROUP(4, "技能组"),
    VOICE(5, "放音"),
    IVR(6, "ivr"),
    EXTENSION(7, "座机"),
    AI(8, "智能坐席"),

    ;

    //路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-ivr 7-座机 8-智能坐席(AI-callbot)
    private Integer type;

    private String desc;

    RouteTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
