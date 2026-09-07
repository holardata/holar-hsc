package com.hsc.system.domain.vo.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作台「最近通话」快照（myRecent 接口返回，6 条倒序）。
 */
@Schema
@Data
public class MyRecentVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "话单ID")
    private Long id;

    @Schema(description = "呼叫唯一ID")
    private String callId;

    @Schema(description = "对端号码(呼入=主叫/呼出=被叫,脱敏由前端处理)")
    private String phone;

    @Schema(description = "方向 1-呼出 2-呼入")
    private Integer direction;

    @Schema(description = "是否接通(answer_flag=0)")
    private Boolean answered;

    @Schema(description = "是否 AI 转接(transfer_human=1)")
    private Boolean aiTransfer;

    @Schema(description = "通话时长(秒,未接通为0)")
    private Long durationSec;

    @Schema(description = "呼叫开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
}
