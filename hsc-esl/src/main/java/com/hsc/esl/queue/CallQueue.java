// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.queue;

import lombok.Builder;
import lombok.Data;

/**
 * @author danmo
 * @date 2024-11-16 14:22
 **/
@Builder
@Data
public class CallQueue implements Comparable<CallQueue> {
    /**
     * 1-普通 2-VIP
     */
    private Integer type;

    /**
     * 通话ID
     */
    private Long callId;

    /**
     * 进入时间
     */
    private Long startTime;

    /**
     * 技能组ID
     */
    private Long skillId;

    /**
     * 腿Id
     */
    private String uniqueId;

    /**
     * fs地址
     */
    private String address;

    /**
     * 播放排队音标识
     */
    private Boolean playFlag;

    /**
     * 排队音播放名（入队时经 IVoiceFileService.getPlayName 解析缓存，扫描线程零查库；未配兜底 queue.wav）
     */
    private String voiceName;

    /**
     * 周期播报音播放名（入队时解析缓存；未配为 null=不播报）
     */
    private String announceVoiceName;

    /**
     * 上次周期播报时间（fsAcd 扫描时距此 ≥30s 触发下一次）
     */
    private Long lastAnnounceTime;

    @Override
    public int compareTo(CallQueue o) {
        // FIFO：先排队的先出队（PriorityQueue 取 compareTo 小者；原 o.compareTo(this) 是后进先出）
        return this.startTime.compareTo(o.startTime);
    }
}
