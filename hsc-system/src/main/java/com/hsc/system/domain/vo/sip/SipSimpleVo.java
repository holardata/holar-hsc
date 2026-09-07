// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.sip;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class SipSimpleVo {

    @Schema(description = "sipId")
    private Integer sipId;

    @Schema(description = "sip名称")
    private String sipName;

    @Schema(description = "终端类型 0-软电话 1-座机")
    private Integer terminalType;
}
