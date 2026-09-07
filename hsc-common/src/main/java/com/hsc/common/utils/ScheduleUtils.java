package com.hsc.common.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日程时间命中判断工具。
 *
 * <p>供呼入路由({@code FsAbstractCallProcess#checkSchedule})等场景判断"某时刻是否命中一条日程"。
 * 支持跨夜时段(startTime &gt; endTime)，周期(workCycle)按班次开始日归属。
 *
 * @author pangshuai
 */
public class ScheduleUtils {

    private ScheduleUtils() {
    }

    /**
     * 解析周期串为 ISO 周几集合。
     *
     * <p>对齐 DB 层 {@code find_in_set} 的精确成员语义，避免 {@code String.contains} 子串误匹配
     * (如 "1,11" 被 "1" 误命中)。
     *
     * @param cycle 逗号分隔的周几串，如 "1,2,3,4,5"（1=周一..7=周日）
     * @return 周几集合；null/空串返回空集合
     */
    public static Set<Integer> parseWorkCycle(String cycle) {
        if (cycle == null || cycle.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(cycle.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    /**
     * 取某日的 ISO 周几(1=周一..7=周日)。
     *
     * @param date 日期
     * @return 1=周一..7=周日
     */
    public static int isoDayOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int calDow = cal.get(Calendar.DAY_OF_WEEK); // 1=周日..7=周六
        return calDow == Calendar.SUNDAY ? 7 : calDow - 1; // 转 ISO: 1=周一..7=周日
    }

    /**
     * 判断当前时刻落在哪个班次开始日，返回相对今天的偏移。
     *
     * <p>用于跨夜班次的开始日定位（周期按开始日归属）：
     * <ul>
     *   <li>日间(startTime &le; endTime)：当前时刻在 [start,end] 内 → {@code 0}(今天)；否则 {@code null}(不命中)。</li>
     *   <li>跨夜(startTime &gt; endTime)：当前时刻 &ge; start → {@code 0}(今天，前半段)；
     *       当前时刻 &le; end → {@code -1}(昨天，后半段)；否则 {@code null}(空档)。</li>
     * </ul>
     *
     * @param nowHHmm   当前时刻 "HH:mm"，非空
     * @param startHHmm 班次开始时刻 "HH:mm"，非空
     * @param endHHmm   班次结束时刻 "HH:mm"，非空
     * @return {@code 0}=今天，{@code -1}=昨天，{@code null}=时段不命中
     */
    public static Integer shiftStartDayOffset(String nowHHmm, String startHHmm, String endHHmm) {
        int now = toMinutes(nowHHmm);
        int start = toMinutes(startHHmm);
        int end = toMinutes(endHHmm);
        if (start <= end) {
            // 日间：[start, end]
            return (start <= now && now <= end) ? 0 : null;
        }
        // 跨夜：[start, 24:00) ∪ [00:00, end]
        if (now >= start) {
            return 0;
        }
        if (now <= end) {
            return -1;
        }
        return null;
    }

    private static int toMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
