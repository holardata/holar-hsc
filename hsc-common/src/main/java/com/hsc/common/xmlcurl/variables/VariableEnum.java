// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.xmlcurl.variables;

import lombok.Getter;

@Getter
public enum VariableEnum {
    /**
     * domain
     */
    DEFAULT_GATEWAY("default_gateway", ""),

    DEFAULT_AREACODE("default_areacode", ""),

    TRANSFER_FALLBACK_EXTENSION("transfer_fallback_extension", ""),

    /**
     * user
     */
    TOLL_ALLOW("toll_allow", ""),

    ACCOUNTCODE("accountcode", ""),

    USER_CONTEXT("user_context", ""),

    EFFECTIVE_CALLER_ID_NAME("effective_caller_id_name", ""),

    EFFECTIVE_CALLER_ID_NUMBER("effective_caller_id_number", ""),

    OUTBOUND_CALLER_ID_NAME("outbound_caller_id_name", ""),

    OUTBOUND_CALLER_ID_NUMBER("outbound_caller_id_number", ""),

    CALLGROUP("callgroup", ""),
    ;

    /**
     * 配置项值
     */
    public final String key;

    /**
     * 配置项说明
     */
    public final String msg;

    VariableEnum(String key, String msg) {
        this.key = key;
        this.msg = msg;
    }

}
