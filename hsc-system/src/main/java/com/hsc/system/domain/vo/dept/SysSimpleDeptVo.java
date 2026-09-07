package com.hsc.system.domain.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门精简信息（用户 deptInfo 回显用）。
 */
@Schema
@Data
public class SysSimpleDeptVo {

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;
}
