package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.DialogRecord;
import com.hsc.system.mapper.DialogRecordMapper;
import com.hsc.system.service.IDialogRecordService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通话逐句对话记录(dialog_record)表服务实现
 */
@Service
public class DialogRecordServiceImpl extends BaseServiceImpl<DialogRecordMapper, DialogRecord> implements IDialogRecordService {

    @Override
    public List<DialogRecord> listAsrByCallId(String callId) {
        return list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getMsgType, "ASR")
                .orderByAsc(DialogRecord::getSpeakTime));
    }

    @Override
    public List<String> listCallIdsWithPendingAsr() {
        List<DialogRecord> list = list(new LambdaQueryWrapper<DialogRecord>()
                .select(DialogRecord::getCallId)
                .eq(DialogRecord::getStatus, 0)
                .eq(DialogRecord::getMsgType, "ASR")
                .groupBy(DialogRecord::getCallId));
        return list.stream().map(DialogRecord::getCallId).distinct().collect(Collectors.toList());
    }

    @Override
    public List<DialogRecord> listPendingAsrByCallId(String callId) {
        return list(new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getStatus, 0)
                .eq(DialogRecord::getMsgType, "ASR")
                .orderByAsc(DialogRecord::getSpeakTime));
    }

    @Override
    public void markProcessed(String callId) {
        DialogRecord update = new DialogRecord();
        update.setStatus(1);
        update(update, new LambdaQueryWrapper<DialogRecord>()
                .eq(DialogRecord::getCallId, callId)
                .eq(DialogRecord::getStatus, 0));
    }
}
