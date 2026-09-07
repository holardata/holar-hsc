package com.hsc.calltask.domain.entity;

import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;


/**
 * 客户流转日志表(CustomerPoolLog)表实体类：归属动作低频事件（导入/分配/收回），只增不改（审计）。
 * 拨打事实复用 call_task_dial_log，客户详情时间线按 customer_id UNION 两表混排展示。
 *
 * @author danmo
 * @since 2026-09-03
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("customer_pool_log")
public class CustomerPoolLog implements Serializable {
  private static final long serialVersionUID = 1L;


    /**
     *  主键ID
     */

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     *  客户ID
     */
    @Schema(description = "客户ID(customer_seas.id)")
    @TableField("customer_id")
    private Long customerId;


    /**
     *  动作 1-导入入库 2-分配 3-收回
     */
    @Schema(description = "动作 1-导入入库 2-分配 3-收回")
    @TableField("action")
    private Integer action;


    /**
     *  原归属坐席（分配时为空）
     */
    @Schema(description = "原归属坐席(分配时空)")
    @TableField("from_agent")
    private Long fromAgent;


    /**
     *  新归属坐席（收回时为空）
     */
    @Schema(description = "新归属坐席(收回时空)")
    @TableField("to_agent")
    private Long toAgent;


    /**
     *  操作管理员（导入事件为空）
     */
    @Schema(description = "操作管理员(user_id,导入事件为空)")
    @TableField("operator")
    private Long operator;


    /**
     *  原因（收回时填写）
     */
    @Schema(description = "原因(收回时填写)")
    @TableField("reason")
    private String reason;


    /**
     *  创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField("create_time")
    private Date createTime;


}
