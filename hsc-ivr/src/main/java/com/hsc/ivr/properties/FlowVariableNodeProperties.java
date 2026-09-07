package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowVariableNodeProperties extends FlowNodeProperties {

    /**
     * 变量赋值列表（逐项 SpEL 求值后写变量袋）。字段名对齐前端 types.ts 的 variableItems。
     */
    private List<VariableItem> variableItems;

    @Data
    public static class VariableItem {
        /**
         * 变量名
         */
        private String key;

        /**
         * 值（字面量或 SpEL 表达式，如 #StrUtil.substring(variables.callerNum,0,3)）
         */
        private String val;
    }
}
