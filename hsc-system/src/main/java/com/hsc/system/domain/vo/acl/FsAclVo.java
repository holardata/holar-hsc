// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.acl;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author danmo
 * @date 2023年09月13日 14:48
 */
@Schema
@Data
public class FsAclVo extends BaseVo {

    @Schema(description = "ID", hidden = true)
    private Long id;

    /**
     * 网关名称
     */
    @Schema(description = "名称")
    private String name;


    /**
     * 类型 allow-允许 deny-拒绝
     */
    @Schema(description = "类型 allow-允许 deny-拒绝")
    private String defaultType;


    @Schema(description = "规则列表")
    private List<FsAclNodeVo> nodeList;

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
