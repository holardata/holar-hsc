// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.subsriber;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2024年07月25日 13:58
 */
@Schema
@Data
public class KoSubscriberQuery extends BaseQuery {

    @Schema(description = "主键ID")
    private Integer id;

    @Schema(description = "SIP号码")
    private String username;

    @Schema(description = "SIP密码")
    private String password;

    @Schema(description = "状态 0-开启 1-关闭")
    private Integer status;

    @Schema(description = "终端类型 0-软电话 1-座机")
    private Integer terminalType;
}
