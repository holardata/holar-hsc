// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.service;


import com.hsc.common.domain.CallInfo;
import com.hsc.system.domain.vo.route.CallRouteVo;

/**
 * @author danmo
 * @date 2023-10-23 16:43
 **/
public interface IFsCallCacheService {

    void saveCallInfo(CallInfo callInfo);

    CallInfo getCallInfo(Long callId);

    CallInfo getCallInfoByUniqueId(String uniqueId);

    void saveCallRel(String uniqueId, Long callId);

    Long getCallId(String uniqueId);

    /**
     * 获取路由信息
     * @param routeNum 路由号码
     * @param type 路由类型 1-呼入 2-呼出
     * @return 路由信息
     */
    CallRouteVo getCallRoute(String routeNum, Integer type);

    /**
     * 获取路由信息（带主叫匹配：caller_num 非空的路由须主叫号 regexp 命中才入选，空=不限）
     * @param routeNum 路由号码（被叫）
     * @param callerNum 主叫号码（呼入路由灰度/VIP 直达用；可空）
     * @param type 路由类型 1-呼入 2-呼出
     * @return 路由信息（命中多条按 level 降序取首条）
     */
    CallRouteVo getCallRoute(String routeNum, String callerNum, Integer type);


    void removeCallInfo(Long callId);
}
