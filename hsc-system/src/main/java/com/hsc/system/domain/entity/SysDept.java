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
 * 部门信息表(sys_dept)实体。
 *
 * <p>基于 hsc 既有 sys_dept 表（bigint 主键/dept_name/order/status），平迁 rb 部门管理业务逻辑。
 * hsc 表与 rb SysDept(pig 框架 String 主键/name/sort/lockFlag) 结构不同，entity 按 hsc 表定义。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("sys_dept")
public class SysDept extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    @TableId(value = "dept_id", type = IdType.AUTO)
    private Long deptId;

    @Schema(description = "部门名称")
    @TableField("dept_name")
    private String deptName;

    @Schema(description = "父ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "显示顺序")
    @TableField("`order`")
    private Integer order;

    @Schema(description = "部门状态 0正常 1停用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;
}
