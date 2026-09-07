package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallInPhoneRel;
import com.hsc.system.domain.query.callin.CallInPhoneRelQuery;

import java.util.List;

/**
 * 呼入号码路由子码表(CallInPhoneRel)表服务接口
 *
 * @author danmo
 * @since 2024-12-10 10:29:11
 */
public interface ICallInPhoneRelService extends IBaseService<CallInPhoneRel> {

    void saveByCallInId(Long callInId, List<CallInPhoneRelQuery> routList);

    void updateByCallInId(Long callInId, List<CallInPhoneRelQuery> routList);

    void delByCallInIds(List<Long> callInIds);
}

