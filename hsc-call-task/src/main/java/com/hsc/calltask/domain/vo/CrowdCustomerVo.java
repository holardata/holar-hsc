// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.vo;


import com.alibaba.fastjson2.JSONObject;
import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人群客户
 * @author danmo
 * @date 2025/6/30 15:39
 */

@EqualsAndHashCode(callSuper = true)
@Schema(description = "人群客户")
@Data
public class CrowdCustomerVo extends BaseVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "客户数据")
    private JSONObject customerInfo;

    @Schema(description = "客户电话")
    private String phone;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "来源 0-手动创建 1-文件导入 2-API导入")
    private Integer source;

    @Schema(description = "删除标志 0-正常 1-删除")
    private Integer delFlag;
}
