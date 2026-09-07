// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.route;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author danmo
 * @date 2023-10-18 14:30
 **/
@Schema
@Data
public class CallRouteAddQuery {

    @Schema(description = "主键ID", hidden = true)
    private Long id;


    /**
     * 路由名称
     */
    @NotBlank(message = "路由名称不能为空")
    @Schema(description = "路由名称")
    private String name;


    /**
     * 路由号码
     */
    @Schema(description = "路由号码")
    @NotBlank(message = "路由号码不能为空")
    private String routeNum;


    /**
     * 主叫号码正则（空=不限主叫，仅呼入路由消费）
     */
    @Schema(description = "主叫号码正则（空=不限主叫）")
    private String callerNum;


    /**
     * 路由类型 1-呼入 2-呼出
     */
    @NotNull(message = "路由类型不能为空")
    @Schema(description = "路由类型 1-呼入 2-呼出 ")
    private Integer type;

    /**
     * 路由等级
     */
    @Schema(description = "路由等级")
    private Integer level;

    /**
     * 日程ID
     */
    @Schema(description = "日程ID")
    private Long scheduleId;

    /**
     * 路由类型
     */
    @Schema(description = "路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-ivr")
    private Integer routeType;

    /**
     * 路由类型值
     */
    @Schema(description = "路由类型值")
    private String routeValue;

}
