// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.agent;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2023年09月26日 13:41
 */
@Schema
@Data
public class SipAgentVo {


    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "坐席名称")
    private String name;

    @Schema(description = "员工ID")
    private Long userId;

    @Schema(description = "员工名称")
    private String userName;

    @Schema(description = "员工姓名（sys_user.nick_name）")
    private String nickName;

    @Schema(description = "SIP号码")
    private String agentNumber;


    @Schema(description = "状态 0-未开通 1-开通")
    private Integer status;
    /**
     * 状态  1-空闲 2-忙碌 3-勿扰 4-离线 5-通话中 6-振铃中 7-话后
     */
    @Schema(description = "在线状态  1-空闲 2-忙碌 3-勿扰 4-离线 5-通话中 6-振铃中 7-话后")
    private Integer onlineStatus;

    @Schema(description = "终端类型 0-软电话 1-座机（按 agentNumber 关联 ko_subscriber.terminal_type）")
    private Integer terminalType;
}
