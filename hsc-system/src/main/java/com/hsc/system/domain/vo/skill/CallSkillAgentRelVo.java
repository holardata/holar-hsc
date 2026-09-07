// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.skill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2024-10-31 14:52
 **/
@Schema
@Data
public class CallSkillAgentRelVo {

    @Schema(description = "主键ID")
    private Long id;
    /**
     *  坐席ID
     */
    @Schema(description = "坐席ID")
    private Long agentId;

    @Schema(description = "成员名称(坐席名/座机号)")
    private String agentName;

    /**
     * 成员类型 0-坐席 1-座机
     */
    @Schema(description = "成员类型 0-坐席 1-座机")
    private Integer memberType;

    /**
     *  级别
     */
    @Schema(description = "级别")
    private Integer level;
}
