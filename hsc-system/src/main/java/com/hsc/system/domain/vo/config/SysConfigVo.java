package com.hsc.system.domain.vo.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统参数列表行
 *
 * @author pangshuai
 * @date 2026-09-01
 */
@Schema
@Data
public class SysConfigVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "参数键(只读)")
    private String configKey;

    @Schema(description = "参数名(只读)")
    private String configName;

    @Schema(description = "参数值")
    private String configValue;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private Date updateTime;
}
