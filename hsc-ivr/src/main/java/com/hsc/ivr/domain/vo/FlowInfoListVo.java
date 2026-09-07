// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.domain.vo;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class FlowInfoListVo extends BaseVo {

    @Schema(description = "流程实例唯一标识符")
    private Long id;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "ivr名称")
    private String name;

    @Schema(description = "流程描述")
    private String desc;

    @Schema(description = "流程状态 0-草稿 1-待发布 2-已发布")
    private Integer status;
}
