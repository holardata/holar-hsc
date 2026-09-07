// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 筛选关系枚举
 *
 * @author danmo
 * @date 2025/06/30
 */
@Getter
public enum RelationEnum {

    // 列名 customer_info 必须内联在 SQL 里：apply 的 {N} 是绑定参数，列名传参会被当字符串常量
    // （JSON_EXTRACT('customer_info',...) 报 Invalid JSON text）。字段名 {0} 保持绑定参数防注入，
    // 用 CONCAT 拼进 JSON path。取值包 JSON_UNQUOTE：customer_info 里值均按字符串存储，
    // 不去引号时纯数字串（手机号等）会被 MySQL 按 JSON 数字解析导致比较永远不成立。
    EQUAL(1, "等于", "{0} = {1}", "JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) = {1}"),
    NOT_EQUAL(2, "不等于", "{0} != {1}", "JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) != {1}"),
    MORE_THAN(3, "大于", "{0} > {1}", "CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) > {1}"),
    GREATER_EQUAL(4, "大于等于", "{0} >= {1}", "CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) >= {1}"),
    LESS_THAN(5, "小于", "{0} < {1}", "CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) < {1}"),
    LESS_EQUAL(6, "小于等于", "{0} <= {1}", "CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) <= {1}"),
    INTERVAL(7, "区间", "{0} >= {1} and {0} <= {2}", "CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) >= {1} and CAST(JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) AS DECIMAL(20,6)) <= {2}"),
    NULL(8, "为空", "{0} is null", "JSON_EXTRACT(customer_info, CONCAT('$.', {0})) is null"),
    NOT_NULL(9, "不为空", "{0} is not null", "JSON_EXTRACT(customer_info, CONCAT('$.', {0})) is not null"),
    INCLUDE(10, "包含", "{0} like '%{1}%'", "JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) like CONCAT('%', {1}, '%')"),
    NOT_INCLUDE(11, "不包含", "{0} not like '%{1}%'", "JSON_UNQUOTE(JSON_EXTRACT(customer_info, CONCAT('$.', {0}))) not like CONCAT('%', {1}, '%')"),

    ;

    private final Integer code;

    private final String value;

    private final String format;

    //sql查询json中信息
    private final String jsonFormat;


    RelationEnum(Integer code, String value, String format, String jsonFormat) {
        this.code = code;
        this.value = value;
        this.format = format;
        this.jsonFormat = jsonFormat;
    }

    private static final Map<Integer, RelationEnum> ENUM_MAP = new HashMap<>();
    private static final Map<Integer, String> VALUE_MAP = new HashMap<>();
    private static final Map<Integer, String> JSON_ENUM_MAP = new HashMap<>();
    private static final Map<Integer, String> SQL_ENUM_MAP = new HashMap<>();

    static {
        for (RelationEnum item : RelationEnum.values()) {
            ENUM_MAP.put(item.getCode(), item);
            VALUE_MAP.put(item.getCode(), item.getValue());
            JSON_ENUM_MAP.put(item.getCode(), item.getJsonFormat());
            SQL_ENUM_MAP.put(item.getCode(), item.getFormat());
        }
    }

    public static RelationEnum getEnum(Integer code) {
        return ENUM_MAP.getOrDefault(code, null);
    }

    public static String getValue(Integer code) {
        return VALUE_MAP.getOrDefault(code, "");
    }

    public static String getJsonValue(Integer code) {
        return JSON_ENUM_MAP.getOrDefault(code, "");
    }

    public static String getSqlValue(Integer code) {
        return SQL_ENUM_MAP.getOrDefault(code, "");
    }

}
