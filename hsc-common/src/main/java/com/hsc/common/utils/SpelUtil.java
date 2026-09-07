package com.hsc.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdcardUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IVR 流程 SpEL 表达式求值工具（IVR 引擎档2 变量系统地基）。
 *
 * <p>两类求值入口（语法分离，勿混用）：
 * <ul>
 *   <li>条件/赋值：{@link #parse} 用 SpEL 原生语法（面向技术配置），如
 *       {@code #StrUtil.equals(variables.vip,'1')}，hutool 工具类注册为 {@code #工具名} 变量。</li>
 *   <li>放音混播：{@link #renderTemplate} 用 {@code ${变量名}} 模板语法（面向业务配置），
 *       缺变量替空串，不中断播放。</li>
 * </ul>
 *
 * <p>借鉴 SmartCall NodeContext 的工具函数注册，但用 Spring 原生 SpelExpressionParser
 * （不引入 SmartCall 的 JpowerSpelExpressionParser 外部 jar）。
 *
 * @author pangshuai
 */
public class SpelUtil {

    private SpelUtil() {
    }

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /** 表达式缓存（流程条件表达式重复求值频繁） */
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    /** 混播占位符：${变量名}，变量名允许字母数字下划线点号 */
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([\\w.]+)}");

    /**
     * SpEL 表达式求值（条件判断/变量赋值用，原生 SpEL 语法）。
     *
     * @param expr       表达式，如 {@code #StrUtil.equals(variables.vip,'1')}
     * @param rootObject 求值根对象（通常是含 variables 的 FlowDataContext）
     * @param returnType 期望返回类型
     * @return 求值结果；表达式为空或求值异常返回 null
     */
    public static <T> T parse(String expr, Object rootObject, Class<T> returnType) {
        if (StrUtil.isBlank(expr)) {
            return null;
        }
        try {
            Expression expression = EXPRESSION_CACHE.computeIfAbsent(expr.trim(), SpelUtil::parseSafe);
            StandardEvaluationContext context = buildContext(rootObject);
            return expression.getValue(context, rootObject, returnType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * SpEL 表达式求值为 Boolean（条件分支判断用，求值失败视为不命中）。
     *
     * @param expr       条件表达式
     * @param rootObject 求值根对象
     * @return 求值结果；空表达式/求值异常返回 false
     */
    public static boolean parseBoolean(String expr, Object rootObject) {
        Boolean result = parse(expr, rootObject, Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 放音文本变量混播（业务配置用 ${变量名} 模板语法）。
     *
     * <p>将文本中所有 {@code ${xxx}} 占位符替换为 SpEL 求值结果；变量不存在或求值失败替空串，
     * 不中断播放。
     *
     * @param text      原始文本，如 {@code 您的余额是 ${balance} 元}
     * @param variables 变量袋
     * @return 替换后文本；text 为空返回原样
     */
    public static String renderTemplate(String text, Map<String, Object> variables) {
        if (StrUtil.isBlank(text) || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = getVariable(variables, key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 构建求值上下文：注册 hutool 工具类为 {@code #工具名} 变量。
     */
    private static StandardEvaluationContext buildContext(Object rootObject) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        context.setVariable("StrUtil", StrUtil.class);
        context.setVariable("ObjectUtil", ObjectUtil.class);
        context.setVariable("NumberUtil", NumberUtil.class);
        context.setVariable("PhoneUtil", PhoneUtil.class);
        context.setVariable("IdcardUtil", IdcardUtil.class);
        context.setVariable("CollUtil", CollUtil.class);
        return context;
    }

    /**
     * parseExpression 抛受检 ParseException，不能直接做 computeIfAbsent 的方法引用，包一层。
     */
    private static Expression parseSafe(String expr) {
        try {
            return PARSER.parseExpression(expr);
        } catch (ParseException e) {
            throw new IllegalArgumentException("SpEL 表达式语法错误: " + expr, e);
        }
    }

    /**
     * 按 ${变量名} 的 key 取变量（支持点号路径，如 user.name 逐层取 Map）。
     */
    private static Object getVariable(Map<String, Object> variables, String key) {
        Object value = variables;
        for (String part : key.split("\\.")) {
            if (!(value instanceof Map<?, ?> map)) {
                return null;
            }
            value = map.get(part);
            if (value == null) {
                return null;
            }
        }
        return value;
    }
}
