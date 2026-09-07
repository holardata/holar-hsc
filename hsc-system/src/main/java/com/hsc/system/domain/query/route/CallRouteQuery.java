// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.route;

import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author danmo
 * @date 2023-10-18 14:34
 **/
@Schema
@Data
public class CallRouteQuery extends BaseQuery {

    @Schema(description = "ID")
    private Long id;

    private List<Long> ids;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "路由号码")
    private String routeNumber;

    /** 呼入匹配用的主叫号（getList 按其 regexp caller_num 过滤，字段不进管理台查询表单） */
    @Schema(description = "主叫号码（路由匹配用）", hidden = true)
    private String callerNumber;

    @Schema(description = "路由类型  1-呼入 2-呼出")
    private Integer type;

    @Schema(description = "路由优先级")
    private Integer level;

    @Schema(description = "状态  0-未启用 1-启用")
    private Integer status;

    @Schema(description = "呼出路由类型 路由类型 1-坐席 2-外呼 3-sip 4-技能组 5-放音 6-ivr")
    private Integer routeType;


}
