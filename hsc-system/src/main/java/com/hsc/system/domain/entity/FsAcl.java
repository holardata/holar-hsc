// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
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
 * fs访问控制表(FsAcl)
 *
 * @author danmo
 * @date 2023-09-13 13:53:45
 */
@Schema
@Data
@SuppressWarnings("serial")
@TableName("fs_acl")
public class FsAcl extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L; //1

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 网关名称
     */
    @Schema(description = "网关名称")
    @TableField("name")
    private String name;


    /**
     * 类型 allow-允许 deny-拒绝
     */
    @Schema(description = "类型 allow-允许 deny-拒绝")
    @TableField("default_type")
    private String defaultType;


    /**
     * 列表ID
     */
    @Schema(description = "列表ID")
    @TableField("list_id")
    private Long listId;


    /**
     * 规则类型 allow-允许 deny-拒绝
     */
    @Schema(description = "规则类型 allow-允许 deny-拒绝")
    @TableField("node_type")
    private String nodeType;


    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    @TableField("cidr")
    private String cidr;


    /**
     * 域地址
     */
    @Schema(description = "域地址")
    @TableField("domain")
    private String domain;


    /**
     * 中文显示名(界面展示用,不影响 FS 下发:FsAclXmlCurlHandler 只读 name/defaultType/node 字段)
     */
    @Schema(description = "中文显示名")
    @TableField("remark")
    private String remark;

    /**
     * 用途说明(仅 list 级行使用:管哪个口/作用/改动后果;节点级说明用 remark,不影响 FS 下发)
     */
    @Schema(description = "用途说明")
    @TableField("description")
    private String description;

}
