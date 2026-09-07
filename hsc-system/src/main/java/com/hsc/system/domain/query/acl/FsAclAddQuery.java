// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.query.acl;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * @author danmo
 * @date 2023年09月11日 15:01
 */
@Schema
@Data
public class FsAclAddQuery {

    @Schema(description = "ID", hidden = true)
    private Long id;

    /**
     * 网关名称(英文标识,list 锁死后编辑不更新 name;新增已禁用)
     */
    @Schema(description = "名称")
    private String name;


    /**
     * 类型 allow-允许 deny-拒绝
     */
    @NotEmpty(message = "类型不能为空")
    @Schema(description = "类型 allow-允许 deny-拒绝",requiredMode = Schema.RequiredMode.REQUIRED)
    private String defaultType;

    /**
     * 中文显示名(界面展示用)
     */
    @Schema(description = "中文显示名")
    private String remark;

    /**
     * 用途说明(管哪个口/作用/改动后果,仅 list 级)
     */
    @Schema(description = "用途说明")
    private String description;
}
