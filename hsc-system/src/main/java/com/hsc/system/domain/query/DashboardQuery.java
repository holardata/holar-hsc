package com.hsc.system.domain.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统计看板统一入参（/system/v1/dashboard/* 全部接口共用）。
 * startTime/endTime 为闭开区间端点，格式 yyyy-MM-dd HH:mm:ss；
 * level/province 仅 region 接口使用（province=全国图下钻时的省全名，对齐 GeoJSON 命名）。
 */
@Schema
@Data
public class DashboardQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "归属地层级 province=全国省级(默认) city=省内市级")
    private String level;

    @Schema(description = "省全名(level=city 时必传,如'广东省')")
    private String province;
}
