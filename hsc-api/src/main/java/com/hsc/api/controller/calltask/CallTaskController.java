// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.calltask;


import com.github.pagehelper.PageInfo;
import com.hsc.calltask.domain.query.CallTaskAddQuery;
import com.hsc.calltask.domain.query.CallTaskContactImportQuery;
import com.hsc.calltask.domain.query.CallTaskContactQuery;
import com.hsc.calltask.domain.query.CallTaskDialLogQuery;
import com.hsc.calltask.domain.query.CallTaskDialQuery;
import com.hsc.calltask.domain.query.CallTaskDispositionQuery;
import com.hsc.calltask.domain.query.CallTaskQuery;
import com.hsc.calltask.domain.vo.CallTaskContactVo;
import com.hsc.calltask.domain.vo.CallTaskDialLogVo;
import com.hsc.calltask.domain.vo.CallTaskMySummaryVo;
import com.hsc.calltask.domain.vo.CallTaskVo;
import com.hsc.calltask.service.ICallTaskService;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 呼叫任务管理
 *
 * @author danmo
 * @date 2025/06/19 09:53
 */
@Tag(name = "呼叫任务管理")
@RestController
@RequestMapping("/call/task/v1")
public class CallTaskController extends BaseController {

    @Autowired
    private ICallTaskService callTaskService;

    @Log(title = "新增呼叫任务", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('call:task:add')")
    @Operation(summary = "新增呼叫任务", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated CallTaskAddQuery query) {
        callTaskService.add(query);
        return success();
    }

    @Log(title = "修改呼叫任务", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:task:edit')")
    @Operation(summary = "修改呼叫任务", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated CallTaskAddQuery query) {
        query.setId(id);
        callTaskService.edit(query);
        return success();
    }

    @Log(title = "删除呼叫任务", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('call:task:delete')")
    @Operation(summary = "删除呼叫任务", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody CallTaskQuery query) {
        callTaskService.detele(query);
        return success();
    }

    @Log(title = "呼叫任务详情", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "呼叫任务详情", method = "POST")
    @PreAuthorize("@authz.hasPerm('call:task:get')")
    @PostMapping("/get/{id}")
    public ResResult<CallTaskVo> get(@PathVariable("id") Long id) {
        return success(callTaskService.getDetail(id));
    }

    @Log(title = "呼叫任务列表(分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:task:page:list')")
    @Operation(summary = "呼叫任务列表(分页)", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<CallTaskVo>> pageList(@RequestBody CallTaskQuery query) {
        List<CallTaskVo> list = callTaskService.pageList(query);
        return success(new PageInfo<>(list));
    }

    @Log(title = "呼叫任务列表", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "呼叫任务列表", method = "POST")
    @PostMapping("/list")
    public ResResult<List<CallTaskVo>> list(@RequestBody CallTaskQuery query) {
        List<CallTaskVo> list = callTaskService.getList(query);
        return success(list);
    }

    @Log(title = "开始任务", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:task:start')")
    @Operation(summary = "开始任务", method = "POST")
    @PostMapping("/start/{id}")
    public ResResult start(@PathVariable("id") Long id) {
        callTaskService.startTask(id);
        return success();
    }

    @Log(title = "暂停任务", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:task:pause')")
    @Operation(summary = "暂停任务", method = "POST")
    @PostMapping("/pause/{id}")
    public ResResult pause(@PathVariable("id") Long id) {
        callTaskService.pauseTask(id);
        return success();
    }

    @Log(title = "结束任务", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:task:end')")
    @Operation(summary = "结束任务", method = "POST")
    @PostMapping("/end/{id}")
    public ResResult end(@PathVariable("id") Long id) {
        callTaskService.endTask(id);
        return success();
    }

    @Log(title = "任务联系人列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:task:contact:list')")
    @Operation(summary = "任务联系人列表(分页)", method = "POST")
    @PostMapping("/task/customer/list")
    public ResResult<PageInfo<CallTaskContactVo>> taskContactList(@RequestBody CallTaskContactQuery query) {
        List<CallTaskContactVo> list = callTaskService.getTaskContactPageList(query);
        return success(new PageInfo<>(list));
    }

    @Log(title = "导入任务联系人", businessType = BusinessTypeEnum.IMPORT)
    @PreAuthorize("@authz.hasPerm('call:task:contact:import')")
    @Operation(summary = "导入任务联系人", method = "POST")
    @PostMapping("/task/customer/import")
    public ResResult importTaskContact(CallTaskContactImportQuery query, @RequestParam(value = "file", required = false) MultipartFile file) {
        // multipart 下 @RequestBody 无法解析 form 字段（文件导入必失败的根因）：
        // 去掉注解走无注解对象绑定，前端平铺 FormData 的 taskId/importType/crowdId/templateId 自动注入；
        // 人群导入(importType=0)不传文件，file 可空，Service 文件分支自行校验非空
        return success(callTaskService.importTaskContact(query, file));
    }

    @Log(title = "我的任务联系人", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:task:contact:my')")
    @Operation(summary = "我的待拨联系人(分页,坐席视角)", method = "POST")
    @PostMapping("/task/customer/my/list")
    public ResResult<PageInfo<CallTaskContactVo>> myTaskContactList(@RequestBody CallTaskContactQuery query) {
        // 坐席身份由服务端按登录用户解析（userId→agentId），仅返回分配给本人的联系人
        List<CallTaskContactVo> list = callTaskService.getMyTaskContactPageList(query, SecurityUtils.getUserId());
        return success(new PageInfo<>(list));
    }

    @Log(title = "我的任务汇总", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:task:contact:my')")
    @Operation(summary = "我的任务汇总(坐席视角,仅进行中任务,分配总数/未拨数)", method = "POST")
    @PostMapping("/task/my/summary")
    public ResResult<List<CallTaskMySummaryVo>> myTaskSummary() {
        // 工作台「任务」tab 一级列表：与 my/list 同为坐席视角，复用其 perms
        return success(callTaskService.getMyTaskSummaryList(SecurityUtils.getUserId()));
    }

    @Log(title = "拨打任务联系人", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:task:contact:dial')")
    @Operation(summary = "发起任务联系人拨打(归属校验,呼叫由前端携带身份发起)", method = "POST")
    @PostMapping("/task/customer/dial")
    public ResResult<String> dialTaskContact(@RequestBody CallTaskDialQuery query) {
        // 返回被叫号码：软电话模式前端携带身份标识头直呼，座机模式据此调 /v1/call/create(带 assignmentId) 代拨
        return success(callTaskService.dialTaskContact(query.getAssignmentId(), SecurityUtils.getUserId()));
    }

    @Log(title = "联系人拨打历史", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:task:contact:diallog')")
    @Operation(summary = "任务联系人拨打历史(分页,assignment/customer/task维度)", method = "POST")
    @PostMapping("/task/customer/diallog")
    public ResResult<PageInfo<CallTaskDialLogVo>> assignmentDialLog(@RequestBody CallTaskDialLogQuery query) {
        List<CallTaskDialLogVo> list = callTaskService.getDialLogPageList(query);
        return success(new PageInfo<>(list));
    }

    @Log(title = "话后小结提交", businessType = BusinessTypeEnum.UPDATE)
    @Operation(summary = "补记本通通话的话后结果与备注(坐席本人经手的通话,登录即可)", method = "POST")
    @PostMapping("/task/customer/diallog/disposition")
    public ResResult submitDisposition(@RequestBody CallTaskDispositionQuery query) {
        callTaskService.submitDisposition(query, SecurityUtils.getUserId());
        return success();
    }


}
