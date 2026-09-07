// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.hsc.api.service.IFsXmlCdrService;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.Cdr;
import com.hsc.common.exception.ParserException;
import com.hsc.common.parser.CdrParser;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.FsCdr;
import com.hsc.system.service.IFsCdrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
public class FsXmlCdrServiceImpl implements IFsXmlCdrService {


    @Autowired
    private RedisService redisService;
    @Autowired
    private IFsCdrService iFsCdrService;


    @Async
    @Override
    public void cdrHandler(String reqText) throws ParserException {
        Cdr cdr = CdrParser.decodeThenParse(reqText);
        log.info("cdr 消息：{}", JSONObject.toJSONString(cdr));
        FsCdr fsCdr = parseToCdr(cdr);
        if (Objects.nonNull(fsCdr)) {

            CallInfo callInfo = getCallInfo(fsCdr.getCallId());
            if (Objects.nonNull(callInfo)) {
                if (callInfo.getRecordEndTime() != null) {
                    fsCdr.setRecordEndTime(new Date(callInfo.getRecordEndTime()));
                }

            }

            //保存（1.18 FsCdr 落库，按 callId 幂等）
            iFsCdrService.saveOrUpdate(fsCdr);

            //话单通知
            String cdrNotifyUrl = fsCdr.getCdrNotifyUrl();

            // 只有这通电话没有其他活着的通道时，才清理 callInfo。
            // 转人工时 AI 通道先挂、主叫/坐席通道还活着，此时 AI 腿的 CDR 不能删整通 callInfo——
            // 否则主叫腿回 park 后查不到 callInfo，被当成新电话又路由到 AI（callId 变、录音/话单变两条）。
            if (callInfo == null || callInfo.getChannelMap() == null
                    || callInfo.getChannelMap().size() <= 1) {
                removeCallInfo(fsCdr.getCallId());
            }
        }
    }

    private FsCdr parseToCdr(Cdr cdr) {
        String uuid = cdr.getVariables().getVariableTable().get("uuid");

        CallInfo callInfo = getCallInfoByUniqueId(uuid);
        if (Objects.isNull(callInfo)) {
            return null;
        }
        Long callId = callInfo.getCallId();

        FsCdr fsCdr = new FsCdr();

        fsCdr.setCallId(callId);
        fsCdr.setUuid(uuid);
        fsCdr.setPayload(JSONObject.toJSONString(callInfo.getChannelMap()));
        fsCdr.setCdrNotifyUrl(callInfo.getCdrNotifyUrl());
        if (callInfo.getRecordStartTime() != null) {
            fsCdr.setRecordStartTime(new Date(callInfo.getRecordStartTime()));
        }
        if (callInfo.getRecordEndTime() != null) {
            fsCdr.setRecordEndTime(new Date(callInfo.getRecordEndTime()));
        }
        if (callInfo.getRecordTime() != null) {
            fsCdr.setRecordSec(callInfo.getRecordTime().intValue());
        } else if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("record_seconds"))) {
            fsCdr.setRecordSec(Integer.valueOf(cdr.getVariables().getVariableTable().get("record_seconds")));
        }

        fsCdr.setRecord(callInfo.getRecord());
        switch (callInfo.getDirection()) {
            case 1:
                fsCdr.setDirection("inbound");
                break;
            case 2:
                fsCdr.setDirection("outbound");
                break;
            default:
                fsCdr.setDirection(cdr.getVariables().getVariableTable().get("direction"));
                break;
        }
        //fsCdr.setDirection(cdr.getVariables().getVariableTable().get("direction"));
        fsCdr.setSipLocalNetworkAddr(cdr.getVariables().getVariableTable().get("sip_local_network_addr"));
        fsCdr.setSipNetworkIp(cdr.getVariables().getVariableTable().get("sip_network_ip"));
        fsCdr.setCallerIdNumber(callInfo.getCaller());
        fsCdr.setCallerDisplay(callInfo.getCallerDisplay());
        fsCdr.setDestinationNumber(callInfo.getCallee());
        fsCdr.setDestinationDisplay(callInfo.getCalleeDisplay());
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("start_stamp"))) {
            fsCdr.setStartStamp(DateUtil.parseDateTime(cdr.getVariables().getVariableTable().get("start_stamp")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("answer_stamp"))) {
            fsCdr.setAnswerStamp(DateUtil.parseDateTime(cdr.getVariables().getVariableTable().get("answer_stamp")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("end_stamp"))) {
            fsCdr.setEndStamp(DateUtil.parseDateTime(cdr.getVariables().getVariableTable().get("end_stamp")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("progress_stamp"))) {
            fsCdr.setProgressStamp(DateUtil.parseDateTime(cdr.getVariables().getVariableTable().get("progress_stamp")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("bridge_stamp"))) {
            fsCdr.setBridgeStamp(DateUtil.parseDateTime(cdr.getVariables().getVariableTable().get("bridge_stamp")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("duration"))) {
            fsCdr.setDuration(Integer.valueOf(cdr.getVariables().getVariableTable().get("duration")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("answersec"))) {
            fsCdr.setAnswerSec(Integer.valueOf(cdr.getVariables().getVariableTable().get("answersec")));
        }
        if (StringUtils.isNotBlank(cdr.getVariables().getVariableTable().get("billsec"))) {
            fsCdr.setBillSec(Integer.valueOf(cdr.getVariables().getVariableTable().get("billsec")));
        }
        fsCdr.setHangupCause(cdr.getVariables().getVariableTable().get("hangup_cause"));
        fsCdr.setReadCodec(cdr.getVariables().getVariableTable().get("read_codec"));
        fsCdr.setWriteCodec(cdr.getVariables().getVariableTable().get("write_codec"));
        fsCdr.setSipHangupDisposition(cdr.getVariables().getVariableTable().get("sip_hangup_disposition"));
        return fsCdr;
    }

    private void removeCallInfo(Long callId) {
        CallInfo callInfo = getCallInfo(callId);
        if (Objects.nonNull(callInfo)) {
            callInfo.getChannelMap().forEach((key, value) -> {
                redisService.delCacheMapValue(CacheConstants.CALL_REL_MAP_CACHE_KEY, key);
            });
            redisService.deleteObject(StringUtils.format(CacheConstants.CALL_INFO_CACHE_KEY, callId));
        }
    }

    private CallInfo getCallInfo(Long callId) {
        return redisService.getCacheObject(StringUtils.format(CacheConstants.CALL_INFO_CACHE_KEY, callId));
    }

    private CallInfo getCallInfoByUniqueId(String uniqueId) {
        Long callId = getCallId(uniqueId);
        if (Objects.isNull(callId)) {
            return null;
        }
        return getCallInfo(callId);
    }

    private Long getCallId(String uniqueId) {
        return redisService.getCacheMapValue(CacheConstants.CALL_REL_MAP_CACHE_KEY, uniqueId);
    }
}
