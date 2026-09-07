// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.domain.vo;


import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 客户公海VO
 * @author danmo
 * @date 2025/6/30 15:39
 */

@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户公海VO")
@Data
public class CustomerSeasVo extends BaseVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "客户数据")
    private JSONObject customerInfo;

    @Schema(description = "客户电话")
    private String phone;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "来源 0-手动创建 1-文件导入 2-API导入")
    private Integer source;

    /**
     * 归属坐席ID（NULL=公海）
     */
    private Long ownerId;

    /**
     * 最近分配时间（与实体注解对齐：否则退化 Jackson 默认序列化——数字时间戳/ISO UTC，前端显示错乱）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ownerTime;

    /**
     * 最近被外呼时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastDialTime;

    /**
     * 最近外呼结果 1~7（NULL=未外呼）
     */
    private Integer lastDialResult;

    /**
     * 累计被外呼次数
     */
    private Integer dialCount;
}
