// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.enums;

import lombok.Getter;

@Getter
public enum FlowNodeTypeEnum {

    //0-开始 1-结束 2-放音 3-菜单 4-收号 5-转接 7-满意度 8-外部调用节点 9-条件判断 10-变量赋值
    //（6-挂机节点已废弃移除：与结束节点挂机开关功能重复，编号不复用）
    START(0, "FlowStartHandler"),
    END(1, "FlowEndHandler"),
    PLAY(2, "FlowPlaybackHandler"),
    MENU(3, "FlowMenuHandler"),
    RECEIVE(4, "FlowReceiveHandler"),
    TRANSFER(5, "FlowTransferHandler"),
    SATISFACTION(7, "FlowSatisfactionHandler"),
    HTTP(8, "FlowHttpHandler"),
    CONDITION(9, "FlowConditionHandler"),
    VARIABLE(10, "FlowVariableHandler"),
    ;

    private final Integer type;

    private final String handler;

    FlowNodeTypeEnum(Integer type, String handler) {
        this.type = type;
        this.handler = handler;
    }

    public static String getHandler(Integer type) {
        for (FlowNodeTypeEnum value : FlowNodeTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value.getHandler();
            }
        }
        return null;
    }
}
