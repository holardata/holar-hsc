// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author danmo
 * @date 2023-11-10 14:51
 **/
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CallInfoDetail {

    private Long id;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 通话ID
     */
    private Long callId;

    /**
     * 顺序
     */
    private Integer orderNum;

    /**
     * 类型(1:坐席,2:进ivr,3:技能组,4:放音,5:外线 6-SIP)
     */
    private Integer transferType;

    /**
     * 转接ID
     */
    private Long transferId;

    /**
     * 出队列原因:排队挂机或者转坐席
     */
    private String reason;


}
