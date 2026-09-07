package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用名称-计数分布项（未接原因/挂机方向/接听方式/满意度星级/时长与耗时桶共用）。
 */
@Schema
@Data
public class NameCountVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类名称(如'呼出·客户未接'/'5星'/'<30s')")
    private String name;

    @Schema(description = "数量")
    private Long count;

    public NameCountVo() {
    }

    public NameCountVo(String name, Long count) {
        this.name = name;
        this.count = count;
    }
}
