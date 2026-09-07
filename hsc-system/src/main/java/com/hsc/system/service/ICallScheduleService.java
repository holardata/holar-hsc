package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallSchedule;
import com.hsc.system.domain.query.schedule.CallScheduleAddQuery;
import com.hsc.system.domain.query.schedule.CallScheduleQuery;

import java.util.List;

/**
 * 日程安排表(CallSchedule)表服务接口
 *
 * @author danmo
 * @since 2024-08-09 17:14:31
 */
public interface ICallScheduleService extends IBaseService<CallSchedule> {

    void add(CallScheduleAddQuery query);

    void update(CallScheduleAddQuery query);

    void delete(CallScheduleQuery query);

    CallSchedule getDetail(Long id);

    List<CallSchedule> getList(CallScheduleQuery query);

    List<CallSchedule> getPageList(CallScheduleQuery query);
}

