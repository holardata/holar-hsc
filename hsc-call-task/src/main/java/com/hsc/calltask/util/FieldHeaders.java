package com.hsc.calltask.util;

import com.hsc.common.utils.StringUtils;

/**
 * 客户模板导出 Excel 表头工具。
 *
 * 模板导出的表头格式为"显示名(fieldName)"（见 CustomerTemplateServiceImpl.templateDownload），
 * 导入解析时需取括号内的字段标识作为 customer_info JSON 的 key。公海导入与任务联系人导入共用。
 */
public final class FieldHeaders {

    private FieldHeaders() {
    }

    /**
     * 从表头"显示名(fieldName)"解析字段标识；无括号时原样返回（自造表头容错）
     *
     * @param header Excel 表头单元格文本
     * @return 字段标识
     */
    public static String fieldNameOf(String header) {
        if (StringUtils.isBlank(header)) {
            return header;
        }
        int left = header.indexOf('(');
        int right = header.indexOf(')');
        if (left < 0 || right <= left) {
            return header.trim();
        }
        return header.substring(left + 1, right);
    }
}
