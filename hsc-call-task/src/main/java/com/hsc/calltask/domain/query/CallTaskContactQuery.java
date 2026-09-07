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
 * 呼叫任务客户查询参数
 * @author danmo
 * @date 2025/06/19 10:10
 */
@Schema(description = "呼叫任务客户查询参数")
@EqualsAndHashCode(callSuper = true)
@Data
public class CallTaskContactQuery extends BaseQuery {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "来源 0-人群导入 1-文件导入 2-API导入")
    private Integer source;

    @Schema(description = "分配状态 0-未分配 1-已分配")
    private Integer status;

    @Schema(description = "分配坐席")
    private List<Long> agentIds;

    @Schema(description = "分配开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date assignStartTime;

    @Schema(description = "分配结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date assignEndTime;

    @Schema(description = "拨打状态 0-未拨打 1-已拨打")
    private Integer callStatus;




}
