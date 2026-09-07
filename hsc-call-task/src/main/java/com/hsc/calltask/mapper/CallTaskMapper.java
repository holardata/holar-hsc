// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsc.calltask.domain.query.CallTaskContactQuery;
import com.hsc.calltask.domain.query.CallTaskQuery;
import com.hsc.calltask.domain.vo.CallTaskContactVo;
import com.hsc.calltask.domain.vo.CallTaskVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import com.hsc.calltask.domain.entity.CallTask;
import com.hsc.calltask.domain.query.CallTaskDialLogQuery;
import com.hsc.calltask.domain.vo.CallTaskDialLogVo;
import com.hsc.calltask.domain.vo.CallTaskMySummaryVo;
import com.hsc.calltask.domain.vo.TaskProgressVo;

import java.util.List;

/**
 * 外呼任务表(CallTask)表数据库访问层
 *
 * @author danmo
 * @since 2025-07-08 10:42:34
 */
@Repository()
@Mapper
public interface CallTaskMapper extends BaseMapper<CallTask> {

    List<CallTaskVo> getList(CallTaskQuery query);

    List<CallTaskContactVo> getTaskContactList(CallTaskContactQuery query);

    /** 进行中任务的进度聚合（dashboard taskProgress：总数/已拨） */
    List<TaskProgressVo> getTaskProgress();

    /** 坐席任务汇总（工作台任务 tab 一级列表：仅进行中任务，本人分配总数/未拨数） */
    List<CallTaskMySummaryVo> getMyTaskSummary(@Param("agentId") Long agentId);

    List<CallTaskDialLogVo> getDialLogList(CallTaskDialLogQuery query);
}

