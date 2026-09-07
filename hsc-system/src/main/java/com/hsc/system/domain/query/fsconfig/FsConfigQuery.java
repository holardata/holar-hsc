// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.fsconfig;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema
@Data
public class FsConfigQuery extends BaseQuery {

    /**
     * 主键ID
     */
    @Schema(description = "主键ID", hidden = true)
    private Integer id;

    @Schema(description = "主键ID列表")
    private List<Integer> ids;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;


    /**
     * 客户端分组
     */
    @Schema(description = "客户端分组")
    private String group;


    /**
     * 机器地址 IP
     */
    @Schema(description = "机器地址 IP")
    private String ip;

    /**
     * 机器地址端口
     */
    @Schema(description = "机器地址端口")
    private Integer port;



    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;


    /**
     * 超时时间（秒）
     */
    @Schema(description = "超时时间（秒）")
    private Integer outTime;


}
