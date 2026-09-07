// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallRecord;
import com.hsc.system.domain.query.call.CallRecordQuery;
import com.hsc.system.domain.vo.call.CallRecordVo;

import java.util.List;

/**
 * 呼叫记录表(CallRecord)表服务接口
 *
 * @author danmo
 * @since 2024-11-21 11:23:01
 */
public interface ICallRecordService extends IBaseService<CallRecord> {

    List<CallRecordVo> getCallPageList(CallRecordQuery query);

    /**
     * 按 callId 查询通话记录（供 ASR 消息关联）。
     *
     * @param callId 通话ID
     * @return 通话记录；不存在返回 null
     */
    CallRecord getByCallId(String callId);
}

