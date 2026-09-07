// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.acl;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author danmo
 * @date 2023年09月13日 14:48
 */
@Schema
@Data
public class FsAclNodeVo extends BaseVo {

    @Schema(description = "ID")
    private Long id;


    @Schema(description = "节点名称")
    private String name;


    @Schema(description = "listId")
    private Long listId;

    /**
     * 规则类型 allow-允许 deny-拒绝
     */
    @Schema(description = "规则类型 allow-允许 deny-拒绝")
    private String nodeType;


    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    private String cidr;


    /**
     * 域地址
     */
    @Schema(description = "域地址")
    private String domain;


    /**
     * 节点说明(界面展示用,不影响 FS 下发)
     */
    @Schema(description = "节点说明")
    private String remark;

}
