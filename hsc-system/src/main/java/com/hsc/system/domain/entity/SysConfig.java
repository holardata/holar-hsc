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
 * 系统参数表(sys_config)。运营参数(振铃超时/排队兜底/漏接重分配上限/token 有效期/旁路 ASR
 * 开关等)由 liquibase 预置，页面「参数管理」仅可修改 configValue 与 remark，
 * configKey/configName 只读，不支持新增删除。运行时经 ISysConfigService 统一入口读取
 * (本地缓存，保存即刷新实时生效)。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("sys_config")
public class SysConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "参数键(代码引用,只读)")
    @TableField("config_key")
    private String configKey;

    @Schema(description = "参数名(中文展示,只读)")
    @TableField("config_name")
    private String configName;

    @Schema(description = "参数值(页面唯一可编辑列)")
    @TableField("config_value")
    private String configValue;

    @Schema(description = "备注(参数含义与建议值,可编辑)")
    @TableField("remark")
    private String remark;
}
