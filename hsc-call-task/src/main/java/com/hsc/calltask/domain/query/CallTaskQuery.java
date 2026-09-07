// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 呼叫任务查询参数
 * @author danmo
 * @date 2025/06/19 10:10
 */
@Schema(description = "呼叫任务查询参数")
@EqualsAndHashCode(callSuper = true)
@Data
public class CallTaskQuery extends BaseQuery {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "主键ID集合")
    private List<Long> idList;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务状态(0-未开始 1-进行中 2-暂停 3-结束)")
    private Integer status;

    @Schema(description = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startDay;

    @Schema(description = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endDay;

    @Schema(description = "人群ID")
    private List<Long> crowdIds;

    @Schema(description = "自动完成类型(0-是 1-否)")
    private Integer completeType;
}
