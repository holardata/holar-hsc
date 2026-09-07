// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.fsconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class FsConfigAddQuery {

    /**
     * 主键ID
     */
    @Schema(description = "主键ID", hidden = true)
    private Integer id;


    /**
     * 名称
     */
    @Schema(description = "名称",requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;


    /**
     * 客户端分组
     */
    @Schema(description = "客户端分组",requiredMode = Schema.RequiredMode.REQUIRED)
    private String group;


    /**
     * 机器地址 IP
     */
    @Schema(description = "机器地址 IP",requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    /**
     * 机器地址端口
     */
    @Schema(description = "机器地址端口",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer port;



    /**
     * 密码
     */
    @Schema(description = "密码",requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;


    /**
     * 超时时间（秒）
     */
    @Schema(description = "超时时间（秒）")
    private Integer outTime;
}
