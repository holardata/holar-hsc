package com.hsc.ivr.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FlowConditionNodeProperties extends FlowNodeProperties {

    /**
     * 条件分支列表（按 order 排序求值，命中即停；最后一个分支作 else 兜底）
     */
    private List<ConditionBranch> conditions;

    @Data
    public static class ConditionBranch {
        /**
         * 分支标识（流转事件为 next_{branchId}，与出口边 event 对应）
         */
        private String branchId;

        /**
         * 条件表达式（SpEL 原生语法，如 #StrUtil.equals(variables.vip,'1')；"else" 表示兜底分支）
         */
        private String condition;

        /**
         * 排序号（小者优先）
         */
        private Integer order;
    }
}
