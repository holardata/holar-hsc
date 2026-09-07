// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service;

import com.hsc.calltask.domain.query.CallTaskAddQuery;
import com.hsc.calltask.domain.query.CallTaskContactImportQuery;
import com.hsc.calltask.domain.query.CallTaskDialLogQuery;
import com.hsc.calltask.domain.query.CallTaskDispositionQuery;
import com.hsc.calltask.domain.vo.CallTaskDialLogVo;
import com.hsc.calltask.domain.vo.CallTaskContactImportResultVo;
import com.hsc.calltask.domain.query.CallTaskContactQuery;
import com.hsc.calltask.domain.query.CallTaskQuery;
import com.hsc.calltask.domain.vo.CallTaskContactVo;
import com.hsc.calltask.domain.vo.CallTaskVo;
import com.hsc.calltask.domain.vo.CallTaskMySummaryVo;
import com.hsc.calltask.domain.vo.TaskProgressVo;
import com.hsc.common.base.IBaseService;
import com.hsc.calltask.domain.entity.CallTask;
import com.hsc.common.enums.CallTaskStatusEnum;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 外呼任务表(CallTask)表服务接口
 *
 * @author danmo
 * @since 2025-07-08 10:42:37
 */
public interface ICallTaskService extends IBaseService<CallTask> {

    void add(CallTaskAddQuery query);


    void edit(CallTaskAddQuery query);

    void detele(CallTaskQuery query);

    CallTaskVo getDetail(Long id);

    List<CallTaskVo> pageList(CallTaskQuery query);

    List<CallTaskVo> getList(CallTaskQuery query);

    void startTask(Long id);

    void pauseTask(Long id);

    void endTask(Long id);

    Boolean updateStatus(Long id, CallTaskStatusEnum callTaskStatusEnum);

    List<CallTaskContactVo> getTaskContactPageList(CallTaskContactQuery query);
    List<CallTaskContactVo> getTaskContactList(CallTaskContactQuery query);

    /**
     * 导入任务联系人
     *
     * @param query 查询参数
     * @param file  文件
     */
    CallTaskContactImportResultVo importTaskContact(CallTaskContactImportQuery query, MultipartFile file);

    /**
     * 我的待拨联系人分页：按登录用户绑定的坐席过滤（仅本人已分配），防越权
     *
     * @param query  查询参数（agentIds/status 由服务端覆写，前端传无效）
     * @param userId 登录用户ID（Controller 层取）
     * @return 联系人分页
     */
    List<CallTaskContactVo> getMyTaskContactPageList(CallTaskContactQuery query, Long userId);

    /**
     * 我的任务汇总（工作台任务 tab 一级列表）：仅进行中任务，本人分配总数/未拨数
     *
     * @param userId 登录用户ID（Controller 层取）
     * @return 任务汇总列表（未拨多在前）
     */
    List<CallTaskMySummaryVo> getMyTaskSummaryList(Long userId);

    /**
     * 发起任务联系人拨打：归属校验 + attempt_count+1 + 写 Redis 拨打映射（挂断回写依据）
     *
     * @param assignmentId 联系人记录ID
     * @param userId       登录用户ID（Controller 层取）
     * @return 被叫号码（前端软电话直呼/代拨用）
     */
    String dialTaskContact(Long assignmentId, Long userId);

    /**
     * 拨打历史分页查询（assignment/customer/task 维度）
     */
    List<CallTaskDialLogVo> getDialLogPageList(CallTaskDialLogQuery query);

    /**
     * 话后小结补记（校验为本坐席经手的通话）
     */
    void submitDisposition(CallTaskDispositionQuery query, Long userId);

    /**
     * 进行中任务进度（dashboard taskProgress：总数/已拨/进度；接通无落库字段不做）
     */
    List<TaskProgressVo> getTaskProgress();
}

