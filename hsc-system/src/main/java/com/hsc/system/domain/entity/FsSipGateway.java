// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * SIP网关表(FsSipGateway)表实体类
 *
 * @author danmo
 * @since 2023-10-13 10:29:51
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("fs_sip_gateway")
public class FsSipGateway extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 159716300935496393L;

    /**
     * 主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 网关名称
     */
    @Schema(description = "网关名称")
    @TableField("name")
    private String name;


    /**
     * 账号
     */
    @Schema(description = "账号")
    @TableField("user_name")
    private String userName;


    /**
     * 密码
     */
    @Schema(description = "密码")
    @TableField("password")
    private String password;


    /**
     * 认证地址
     */
    @Schema(description = "认证地址")
    @TableField("realm")
    private String realm;


    /**
     * 代理地址
     */
    @Schema(description = "代理地址")
    @TableField("proxy")
    private String proxy;


    /**
     * 注册类型 0-不注册 1-注册
     */
    @Schema(description = "注册类型 0-不注册 1-注册")
    @TableField("register")
    private Integer register;


    /**
     * 注册协议 1-udp, 2-tcp
     */
    @Schema(description = "注册协议 1-udp, 2-tcp")
    @TableField("transport")
    private Integer transport;


    /**
     * 出站呼叫的 SIP From 头里，是否使用入站呼叫的 callerid。
     * 对应 FS sofia gateway 参数 caller-id-in-from。
     * 取值反直觉：0=true(启用，DB 默认) / 1=false(不启用)。
     * 前端不暴露，由后端默认值 0 下发给 FS（见 FsSipGatewayXmlCurlHandler.getParamList）；
     * 如需开放用户配置，再改前端表单 + Mutation 接口。
     */
    @Schema(description = "出站From头是否用入站callerid(0-启用/1-不启用，默认0，前端不暴露)")
    @TableField("caller_id_in_from")
    private Integer callerIdInFrom;

    /**
     * 出局号码前缀：外呼 originate 时拼在 called 号码前（如填 "0" 走二次拨号）。
     * 仅对出局外线网关(gatewayType=1)生效；转坐席(loopback, gatewayType=0)不应加。
     * 为空表示不加前缀。
     */
    @Schema(description = "出局号码前缀(外呼时拼在called前,如\"0\",空=不加)")
    @TableField("dial_prefix")
    private String dialPrefix;


    /**
     * from域
     */
    @Schema(description = "from域")
    @TableField("from_domain")
    private String fromDomain;


    /**
     * 重试时间（秒）
     */
    @Schema(description = "重试时间（秒）")
    @TableField("retry_time")
    private Integer retryTime;


    /**
     * 心跳时间（秒）
     */
    @Schema(description = "心跳时间（秒）")
    @TableField("ping_time")
    private Integer pingTime;


    /**
     * 超时时间
     */
    @Schema(description = "超时时间(秒)")
    @TableField("expire_time")
    private Integer expireTime;

    @Schema(description = "网关类型 1-internal 2-external")
    @TableField("type")
    private Integer type;

    @Schema(description = "网关类型 0-非外线 1-外线")
    @TableField("gateway_type")
    private Integer gatewayType;
}

