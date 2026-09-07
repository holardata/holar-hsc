// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.websocket.domain;


import lombok.Getter;

/**
 * @author danmo
 * @date 2025/05/27 18:00
 */
@Getter
public enum WsEventEnum {

    //登录
    SUCCEED,
    //心跳
    HEARTBEAT,
    //坐席状态
    AGENT_STATUS,
    //实时转写
    REALTIME_DIALOG,
    //过程摘要
    ABSTRACT,
    //通话状态(开始/结束/转接)
    CALL_STATUS,
    //AI推荐话术(人工坐席助手)
    AGENT_RECOMMEND,
    ;

}
