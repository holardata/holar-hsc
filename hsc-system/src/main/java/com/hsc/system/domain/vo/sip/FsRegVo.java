package com.hsc.system.domain.vo.sip;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * FreeSWITCH SIP 注册项(对应 `sofia status profile internal reg` 的一行)。
 */
@Data
public class FsRegVo {

    @Schema(description = "SIP账号(User)")
    private String username;

    @Schema(description = "contact 地址")
    private String contact;

    @Schema(description = "注册 IP")
    private String ip;

    @Schema(description = "注册端口")
    private String port;

    @Schema(description = "UA 客户端标识(Agent)")
    private String userAgent;

    @Schema(description = "注册过期时间(秒)")
    private String expires;

    @Schema(description = "Call-ID")
    private String callId;

    @Schema(description = "注册状态(Registered/UnRegistered 等)")
    private String status;
}
