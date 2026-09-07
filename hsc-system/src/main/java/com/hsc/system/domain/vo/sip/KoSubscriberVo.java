// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.sip;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KoSubscriberVo extends BaseVo {

    @Schema(description = "id")
    private String id;


    @Schema(description = "Sip名称")
    private String userName;


    @Schema(description = "来源")
    private String domain;


    @Schema(description = "密码")
    private String password;


    @Schema(description = "ha1")
    private String ha1;


    @Schema(description = "ha1b")
    private String ha1b;


    @Schema(description = "vmpin")
    private String vmpin;


    @Schema(description = "状态 0-开启 1-关闭")
    private Integer status;


    @Schema(description = "终端类型 0-软电话 1-座机")
    private Integer terminalType;
}
