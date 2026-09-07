package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 归属地分布项（region 接口返回，省级/市级同构）。
 * name 对齐 DataV GeoJSON 命名（后端完成"广东"→"广东省"映射），前端直接匹配地图。
 */
@Schema
@Data
public class RegionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "省/市全名(对齐地图 GeoJSON,如'广东省'/'深圳市')")
    private String name;

    @Schema(description = "呼入量(该归属地主叫)")
    private Long inbound;

    @Schema(description = "呼出量(该归属地被叫)")
    private Long outbound;

    public RegionVo() {
    }

    public RegionVo(String name, Long inbound, Long outbound) {
        this.name = name;
        this.inbound = inbound;
        this.outbound = outbound;
    }
}
