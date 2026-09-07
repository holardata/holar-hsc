// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "坐席SIP配置")
public class SipAgentConfigVo {

    @Schema(description = "坐席ID（sip_agent.id，软电话上报坐席状态用）")
    private Long agentId;

    @Schema(description = "签入展示名（sip_agent.name + '-' + sys_user.nick_name，软电话签入后展示用）")
    private String agentName;

    @Schema(description = "SIP分机号")
    private String agentNumber;

    @Schema(description = "SIP密码")
    private String password;

    @Schema(description = "SIP域名")
    private String domain;

    @Schema(description = "终端类型 0-软电话 1-座机（座机模式前端不注册 JsSIP，password 不下发）")
    private Integer terminalType;
}
