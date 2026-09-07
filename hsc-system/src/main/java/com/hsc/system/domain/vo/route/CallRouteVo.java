// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.route;

import com.baomidou.mybatisplus.annotation.TableField;
import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2024-12-29 14:35
 **/
@Schema
@Data
public class CallRouteVo extends BaseVo {

    /**
     *
     */

    @Schema(description = "")
    private Long id;


    /**
     * 路由名称
     */
    @Schema(description = "路由名称")
    private String name;


    /**
     * 路由号码
     */
    @Schema(description = "路由号码")
    private String routeNum;


    /**
     * 主叫号码正则（空=不限主叫）
     */
    @Schema(description = "主叫号码正则（空=不限主叫）")
    private String callerNum;


    /**
     * 路由类型 1-呼入 2-呼出
     */
    @Schema(description = "路由类型 1-呼入 2-呼出")
    private Integer type;


    /**
     * 路由优先级
     */
    @Schema(description = "路由优先级")
    private Integer level;


    /**
     * 状态  0-未启用 1-启用
     */
    @Schema(description = "状态  0-未启用 1-启用")
    private Integer status;

    /**
     * 日程ID
     */
    @Schema(description = "日程ID")
    private Long scheduleId;
    /**
     * 日程名称
     */
    @Schema(description = "日程名称")
    private String scheduleName;

    /**
     * 路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-ivr
     */
    @Schema(description = "路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-ivr")
    private Integer routeType;

    /**
     * 路由类型值
     */
    @Schema(description = "路由类型值")
    private String routeValue;
}
