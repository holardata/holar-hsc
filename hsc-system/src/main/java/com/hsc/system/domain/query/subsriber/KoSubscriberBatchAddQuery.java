// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.subsriber;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author danmo
 * @date 2023年09月25日 13:58
 */
@Schema
@Data
public class KoSubscriberBatchAddQuery {

    @Schema(description = "初始值",example = "0000")
    private Integer initNum;

    @Schema(description = "SIP号码长度",requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 8, message = "长度最少8位,最大12位")
    @Max(value = 12, message = "长度最少8位,最大12位")
    private Integer size;

    @Schema(description = "生成号码个数",requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "个数最少1个,最多10个")
    @Max(value = 10, message = "个数最少1个,最多10个")
    private Integer number;

    @Schema(description = "SIP密码（批量默认软电话,密码后端自动生成,此字段忽略）")
    private String password;
}
