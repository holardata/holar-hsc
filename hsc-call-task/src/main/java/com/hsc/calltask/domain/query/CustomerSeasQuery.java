// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.query;


import com.hsc.system.domain.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客户公海查询参数
 * @author danmo
 * @date 2025/6/30 15:37
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户公海查询参数")
@Data
public class CustomerSeasQuery extends BaseQuery {

    @Schema(description = "客户公海ID")
    private Long id;

    @Schema(description = "客户ID列表")
    private List<Long> idList;

    @Schema(description = "模板ID")
    private List<Long> templateIds;

    @Schema(description = "客户电话")
    private String phone;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "来源 0-手动创建 1-文件导入 2-API导入")
    private Integer source;

    @Schema(description = "归属坐席ID过滤（服务端我的客户视图覆写用，公海页不传）")
    private Long ownerId;

    @Schema(description = "仅公海（无归属）客户：公海列表传 true，私海客户不展示")
    private Boolean onlyPublic;
}
