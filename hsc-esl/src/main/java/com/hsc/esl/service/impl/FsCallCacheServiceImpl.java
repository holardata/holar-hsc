// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.system.domain.query.route.CallRouteQuery;
import com.hsc.system.domain.vo.route.CallRouteVo;
import com.hsc.system.service.ICallRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author danmo
 * @date 2023-10-23 16:43
 **/
@Slf4j
@Service
public class FsCallCacheServiceImpl implements IFsCallCacheService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private ICallRouteService iCallRouteService;



    @Override
    public void saveCallInfo(CallInfo callInfo) {
        redisService.setCacheObject(StringUtils.format(CacheConstants.CALL_INFO_CACHE_KEY, callInfo.getCallId()), callInfo);
    }

    @Override
    public CallInfo getCallInfo(Long callId) {
        return redisService.getCacheObject(StringUtils.format(CacheConstants.CALL_INFO_CACHE_KEY, callId));
    }

    @Override
    public CallInfo getCallInfoByUniqueId(String uniqueId) {
        Long callId = getCallId(uniqueId);
        if (Objects.isNull(callId)) {
            return null;
        }
        return getCallInfo(callId);
    }

    @Override
    public void saveCallRel(String uniqueId, Long callId) {
        redisService.setCacheMapValue(CacheConstants.CALL_REL_MAP_CACHE_KEY, uniqueId, callId);
    }

    @Override
    public Long getCallId(String uniqueId) {
        // Number 接收再转 long：Redis 反序列化的数字可能回成 Integer，(Long) 强转会 ClassCastException
        Object callId = redisService.getCacheMapValue(CacheConstants.CALL_REL_MAP_CACHE_KEY, uniqueId);
        return callId instanceof Number ? ((Number) callId).longValue() : null;
    }

    /**
     * 获取路由
     * @param routeNum 路由号码
     * @param type 路由类型 1-呼出 2-呼入
     * @return
     */
    @Override
    public CallRouteVo getCallRoute(String routeNum, Integer type) {
        return getCallRoute(routeNum, null, type);
    }

    /**
     * 获取路由（带主叫匹配）
     */
    @Override
    public CallRouteVo getCallRoute(String routeNum, String callerNum, Integer type) {
        CallRouteQuery routeQuery = new CallRouteQuery();
        routeQuery.setRouteNumber(routeNum);
        routeQuery.setCallerNumber(callerNum);
        routeQuery.setType(type);
        // 只查启用的路由（status=1），未启用的路由(status=0)不参与呼叫分发
        routeQuery.setStatus(1);
        List<CallRouteVo> callRoutes = iCallRouteService.getList(routeQuery);
        if (CollectionUtil.isNotEmpty(callRoutes)) {
            List<CallRouteVo> routeVos = callRoutes.stream().sorted((o1, o2) -> o2.getLevel() - o1.getLevel()).toList();
            return routeVos.get(0);
        }
        return null;
    }

    @Override
    public void removeCallInfo(Long callId) {
        CallInfo callInfo = getCallInfo(callId);
        if (Objects.nonNull(callInfo)) {
            callInfo.getChannelMap().forEach((key, value) -> {
                redisService.delCacheMapValue(CacheConstants.CALL_REL_MAP_CACHE_KEY, key);
            });
            redisService.deleteObject(StringUtils.format(CacheConstants.CALL_INFO_CACHE_KEY, callId));
        }
    }
}
