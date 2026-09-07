// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.domain.vo.display;

import com.hsc.system.domain.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author danmo
 * @date 2023年09月26日 13:44
 */
@EqualsAndHashCode(callSuper = true)
@Schema
@Data
public class CallDisplayVo extends BaseVo {

    /**
     * 主键
     */

    @Schema(description = "主键")
    private Long id;
    /**
     * 电话号码
     */
    @Schema(description = "电话号码")
    private String phone;

    /**
     * 归属地
     */
    @Schema(description = "归属地")
    private String area;
}
