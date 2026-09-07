// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hsc.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * ivr流程信息(FlowInfo)表实体类
 *
 * @author danmo
 * @since 2024-12-23 15:08:24
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("flow_info")
public class FlowInfo extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 676718916120461321L;

    /**
     * 流程实例唯一标识符
     */

    @Schema(description = "流程实例唯一标识符")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 分组ID
     */
    @Schema(description = "分组ID")
    @TableField("group_id")
    private Long groupId;


    /**
     * ivr名称
     */
    @Schema(description = "ivr名称")
    @TableField("name")
    private String name;


    /**
     * 流程描述
     */
    @Schema(description = "流程描述")
    @TableField("`desc`")
    private String desc;


    /**
     * 流程状态 0-草稿 1-待发布 2-已发布
     */
    @Schema(description = "流程状态 0-草稿 1-待发布 2-已发布 3-已下线")
    @TableField("status")
    private Integer status;


    /**
     * 流程数据
     */
    @Schema(description = "流程数据")
    @TableField("flow_data")
    private String flowData;

    /**
     * 已发布版本流程数据快照（发布时从 flow_data 复制；IVR 运行时只读本列，编辑草稿不影响线上）
     */
    @Schema(description = "已发布版本流程数据快照")
    @TableField("published_flow_data")
    private String publishedFlowData;

    /**
     * 发布版本号（每次发布+1；通话进入时钉死，Redis 流程缓存 key 组成部分）
     */
    @Schema(description = "发布版本号")
    @TableField("version")
    private Integer version;
}

