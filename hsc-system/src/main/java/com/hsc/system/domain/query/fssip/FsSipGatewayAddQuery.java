// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.fssip;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * @author danmo
 * @date 2023年09月11日 15:01
 */
@Schema
@Data
public class FsSipGatewayAddQuery {

    @Schema(description = "ID",hidden = true)
    private Long id;


    /**
     * 网关名称
     */
    @NotEmpty(message = "网关名称不能为空")
    @Schema(description = "网关名称",requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;


    /**
     * 账号
     */
    @Schema(description = "账号",requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;


    /**
     * 密码
     */
    @Schema(description = "密码",requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;


    /**
     * 认证地址
     */
    @NotEmpty(message = "认证地址不能为空")
    @Schema(description = "认证地址",requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;


    /**
     * 代理地址
     */
    @NotEmpty(message = "代理地址不能为空")
    @Schema(description = "代理地址",requiredMode = Schema.RequiredMode.REQUIRED)
    private String proxy;


    /**
     * 注册类型 0-不注册 1-注册
     */
    @Schema(description = "注册类型 0-不注册 1-注册",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer register;


    /**
     * 注册协议 1-udp, 2-tcp
     */
    @Schema(description = "注册协议 1-udp, 2-tcp",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer transport;


    /**
     * from域
     */
    @Schema(description = "from域",requiredMode = Schema.RequiredMode.REQUIRED)
    private String fromDomain;

    /**
     * 出局号码前缀(外呼时拼在called前,如"0",空=不加)
     */
    @Schema(description = "出局号码前缀(外呼时拼在called前,如\"0\",空=不加)")
    private String dialPrefix;


    /**
     * 重试时间（秒）
     */
    @Schema(description = "重试时间（秒）",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer retryTime;


    /**
     * 心跳时间（秒）
     */
    @Schema(description = "心跳时间（秒）",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pingTime;

    /**
     * 网关类型 0-非外线 1-外线
     */
    @NotNull(message = "网关类型不能为空")
    @Schema(description = "网关类型 0-非外线 1-外线",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gatewayType;

    /**
     * 挂载 profile：1-internal 2-external（决定网关下发到 FS 的哪个 sofia profile，
     * 见 FsSipGatewayXmlCurlHandler.getProfileList：type=2 挂 external、type=1 挂 internal）
     */
    @NotNull(message = "挂载profile不能为空")
    @Schema(description = "挂载profile 1-internal 2-external",requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;
}
