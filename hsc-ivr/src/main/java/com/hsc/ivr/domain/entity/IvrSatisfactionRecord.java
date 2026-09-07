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
 * IVR满意度评分记录(IvrSatisfactionRecord)表实体类
 *
 * @author pangshuai
 * @since 2026-08-17
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("ivr_satisfaction_record")
public class IvrSatisfactionRecord extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 834501982712989655L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程实例ID")
    @TableField("instance_id")
    private Long instanceId;

    @Schema(description = "通话ID")
    @TableField("call_id")
    private Long callId;

    @Schema(description = "IVR流程ID")
    @TableField("flow_id")
    private Long flowId;

    @Schema(description = "满意度节点ID")
    @TableField("node_id")
    private String nodeId;

    @Schema(description = "评分 1-5(用户按键),0-未评价")
    @TableField("score")
    private Integer score;

    @Schema(description = "用户原始按键")
    @TableField("dtmf")
    private String dtmf;
}
