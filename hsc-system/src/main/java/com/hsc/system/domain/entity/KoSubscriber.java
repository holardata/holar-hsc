// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * (Subscriber) 订阅用户（SIP用户）
 *
 * <p>📌 本表沿袭 Kamailio 的 subscriber 表（Kamailio = 开源 SIP 注册/代理服务器，经典与 FS
 * 配合：Kamailio 前端扛注册+信令路由，FS 后端做媒体/IVR）。其中 domain / vmpin / ha1 / ha1b
 * 都是 Kamailio subscriber 表的专用字段：
 * <ul>
 *   <li>domain：SIP 注册域（Kamailio 多域、算 ha1 用）</li>
 *   <li>vmpin：语音邮箱（voicemail）访问 PIN（Kamailio voicemail 模块）</li>
 *   <li>ha1 / ha1b：Kamailio digest 鉴权预计算 MD5(username:domain:password)</li>
 * </ul>
 *
 * <p>✅ 当前部署：FS 自带 directory（xml_curl 从本表读 password）做注册鉴权，不依赖 ha1、
 * 不读 domain/vmpin → 这些字段闲置，留空不影响 FS 注册。
 *
 * <p>⏳ 何时激活：坐席规模上千 / 多 FS 实例 / 公网高可用 / 复杂信令路由（防 DoS、跨域）时，
 * 引入 Kamailio 做注册中心（它直接读本表），这些字段即生效。当前单 FS 内网规模不需要 Kamailio。
 *
 * @author danmo
 * @date 2024-07-29 10:49:24
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("ko_subscriber")
public class KoSubscriber implements Serializable {

    private static final long serialVersionUID = 1L; //1

    @Schema(description = "id")
    @TableId(type = IdType.AUTO)
    private Integer id;


    @Schema(description = "username")
    @TableField("username")
    private String username;


    @Schema(description = "domain（Kamailio 注册域,当前 FS 用自带 directory、不读此字段,留空即可）")
    @TableField("domain")
    private String domain;


    @Schema(description = "password")
    @TableField("password")
    @JsonIgnore
    private String password;


    @Schema(description = "ha1")
    @TableField("ha1")
    private String ha1;


    @Schema(description = "ha1b")
    @TableField("ha1b")
    private String ha1b;


    @Schema(description = "vmpin（Kamailio voicemail 邮箱 PIN,当前未启用 voicemail,留空即可）")
    @TableField("vmpin")
    private String vmpin;


    @Schema(description = "状态 0-开启 1-关闭")
    @TableField("status")
    private Integer status;


    @Schema(description = "终端类型 0-软电话 1-座机")
    @TableField("terminal_type")
    private Integer terminalType;


    @Schema(description = "创建者")
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @Schema(description = "更新者")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
