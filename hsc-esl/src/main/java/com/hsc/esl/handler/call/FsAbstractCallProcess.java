// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.esl.handler.call;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.hsc.common.constant.SysSettingConfig;
import com.hsc.common.domain.CallInfo;
import com.hsc.common.domain.ChannelInfo;
import com.hsc.common.utils.ScheduleUtils;
import com.hsc.esl.client.FsClient;
import com.hsc.esl.factory.FsEslRouteFactory;
import com.hsc.esl.service.IFsCallCacheService;
import com.hsc.system.domain.entity.CallSchedule;
import com.hsc.system.service.ICallInPhoneService;
import com.hsc.system.service.ICallScheduleService;
import com.hsc.system.service.IFsSipGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

@Slf4j
@Component
public abstract class FsAbstractCallProcess {

    @Lazy
    @Autowired
    protected FsClient fsClient;

    @Autowired
    protected SysSettingConfig sysSettingConfig;

    @Autowired
    protected IFsCallCacheService lfsCallCacheService;

    @Autowired
    protected FsEslRouteFactory routeFactory;

    @Autowired
    protected ICallInPhoneService callInPhoneService;

    @Autowired
    protected IFsSipGatewayService fsSipGatewayService;

    @Autowired
    protected ICallScheduleService callScheduleService;

    public abstract void handler(String address, EslEvent event, CallInfo callInfo, ChannelInfo lfsChannelInfo);

    protected Boolean checkSchedule(CallSchedule scheduleDetail) {
        if (scheduleDetail == null) {
            // 日程已被删但路由仍挂着 scheduleId：按"无合适日程"挂断，不能 NPE 打断路由分发
            return false;
        }
        String workCycle = scheduleDetail.getWorkCycle();
        if (workCycle == null || workCycle.isEmpty()) {
            return false;
        }
        String startTime = scheduleDetail.getStartTime();
        String endTime = scheduleDetail.getEndTime();
        if (startTime == null || startTime.isEmpty() || endTime == null || endTime.isEmpty()) {
            return false;
        }
        Set<Integer> cycle = ScheduleUtils.parseWorkCycle(workCycle);
        // 时段命中 → 定位班次开始日(跨夜时可能是今天或昨天)
        Integer offset = ScheduleUtils.shiftStartDayOffset(
                DateUtil.format(new Date(), "HH:mm"), startTime, endTime);
        if (offset == null) {
            return false;
        }
        DateTime shiftStartDay = DateUtil.offsetDay(DateUtil.date(), offset);
        // 班次开始日须同时满足日期范围与周期(workCycle 按开始日归属)
        return inDayRange(shiftStartDay, scheduleDetail)
                && cycle.contains(ScheduleUtils.isoDayOfWeek(shiftStartDay));
    }

    /** 班次开始日是否落在日程的日期范围内(type=0 绝对日期 / type=1 每月几号)。 */
    private boolean inDayRange(DateTime day, CallSchedule scheduleDetail) {
        if (scheduleDetail.getType() == 0) {
            DateTime startDay = DateUtil.parseDate(scheduleDetail.getStartDay());
            DateTime endDay = DateUtil.parseDate(scheduleDetail.getEndDay());
            return day.isAfterOrEquals(startDay) && day.isBeforeOrEquals(endDay);
        } else if (scheduleDetail.getType() == 1) {
            int dayOfMonth = day.dayOfMonth();
            return Integer.parseInt(scheduleDetail.getStartDay()) <= dayOfMonth
                    && dayOfMonth <= Integer.parseInt(scheduleDetail.getEndDay());
        }
        return false;
    }

}
