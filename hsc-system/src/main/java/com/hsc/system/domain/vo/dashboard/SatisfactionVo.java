package com.hsc.system.domain.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 满意度统计（satisfaction 接口返回，score>0 计入；无数据返回空结构不报错）。
 */
@Schema
@Data
public class SatisfactionVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "均分(1-5,无数据为 null)")
    private BigDecimal avgScore;

    @Schema(description = "有效评价总数")
    private Long total;

    @Schema(description = "星级分布(1星-5星)")
    private List<NameCountVo> distribution;
}
