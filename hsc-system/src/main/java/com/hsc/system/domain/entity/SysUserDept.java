package com.hsc.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户部门关联表(sys_user_dept)实体。
 *
 * <p>多对多：一个用户可属多个部门，一个部门有多个用户。
 * leader 字段表已建（预留负责人），简化版不启用，默认 0。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("sys_user_dept")
public class SysUserDept extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "部门ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "是否负责人 0否 1是（预留，简化版不启用）")
    @TableField("leader")
    private Integer leader;
}
