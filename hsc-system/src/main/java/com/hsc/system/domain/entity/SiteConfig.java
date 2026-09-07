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
 * 站点配置表(site_config，KV 键值对)。OEM 品牌要素存储（appName/logoFileId/faviconFileId/
 * sloganImageFileId/pageTitle/pageDescription/loginSubtitle；图片键存 sys_file.id），
 * 供品牌设置页维护 + 前端启动拉取（登录页登录前展示品牌）。新增品牌键只插数据行、零 DDL；
 * config_value 空串 = 前端回退构建默认值。
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("site_config")
public class SiteConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置键(品牌要素键名)")
    @TableField("config_key")
    private String configKey;

    @Schema(description = "配置值(空串=回退构建默认)")
    @TableField("config_value")
    private String configValue;
}
