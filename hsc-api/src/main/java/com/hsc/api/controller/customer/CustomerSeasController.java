// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.customer;


import com.github.pagehelper.PageInfo;
import com.hsc.calltask.domain.query.CustomerPoolAssignQuery;
import com.hsc.calltask.domain.query.CustomerPoolReleaseQuery;
import com.hsc.calltask.domain.query.CustomerSeasAddQuery;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.domain.vo.CustomerTransferTimelineVo;
import com.hsc.calltask.service.ICustomerPoolService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import com.hsc.security.utils.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户公海管理
 *
 * @author danmo
 * @date 2025/06/16 14:59
 */
@Tag(name = "客户公海管理")
@RestController
@RequestMapping("/customer/v1/seas")
public class CustomerSeasController extends BaseController {

    @Autowired
    private ICustomerSeasService customerSeasService;

    @Autowired
    private ICustomerPoolService customerPoolService;


    @Log(title = "新增公海", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('customer:seas:add')")
    @Operation(summary = "新增公海", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated CustomerSeasAddQuery query) {
        customerSeasService.add(query);
        return success();
    }

    @Log(title = "修改公海", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('customer:seas:edit')")
    @Operation(summary = "修改公海", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated CustomerSeasAddQuery query) {
        query.setId(id);
        customerSeasService.edit(query);
        return success();
    }

    @Log(title = "公海详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('customer:seas:get')")
    @Operation(summary = "公海详情", method = "POST")
    @PostMapping("/get/{id}")
    public ResResult<CustomerSeasVo> get(@PathVariable("id") Long id) {
        return success(customerSeasService.getDetail(id));
    }


    @Log(title = "删除公海", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('customer:seas:delete')")
    @Operation(summary = "删除公海", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody CustomerSeasQuery query) {
        customerSeasService.delete(query);
        return success();
    }

    @Log(title = "公海列表(分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('customer:seas:page:list')")
    @Operation(summary = "公海列表(分页)", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<CustomerSeasVo>> pageList(@RequestBody CustomerSeasQuery query) {
        List<CustomerSeasVo> list = customerSeasService.pageList(query);
        return success(new PageInfo<>(list));
    }

    @Log(title = "公海列表(不分页)", businessType = BusinessTypeEnum.SELECT)
    @Operation(summary = "公海列表(不分页)", method = "POST")
    @PostMapping("/list")
    public ResResult<List<CustomerSeasVo>> list(@RequestBody CustomerSeasQuery query) {
        List<CustomerSeasVo> list = customerSeasService.getList(query);
        return success(list);
    }

    @Log(title = "分配客户给坐席", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('customer:seas:assign')")
    @Operation(summary = "公海客户分配给坐席(进私海,仅无主可分配,乐观锁防并发双分)", method = "POST")
    @PostMapping("/assign")
    public ResResult<Integer> assign(@RequestBody @Validated CustomerPoolAssignQuery query) {
        return success(customerPoolService.assign(query, SecurityUtils.getUserId()));
    }

    @Log(title = "收回私海客户", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('customer:seas:release')")
    @Operation(summary = "收回坐席私海客户回公海(含原因,记流转日志;改派=收回+再分配)", method = "POST")
    @PostMapping("/release")
    public ResResult<Integer> release(@RequestBody @Validated CustomerPoolReleaseQuery query) {
        return success(customerPoolService.release(query, SecurityUtils.getUserId()));
    }

    @Log(title = "我的客户列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('customer:seas:mine')")
    @Operation(summary = "我的客户分页(私海,owner=登录坐席,服务端身份过滤防越权)", method = "POST")
    @PostMapping("/mine/list")
    public ResResult<PageInfo<CustomerSeasVo>> myCustomerList(@RequestBody CustomerSeasQuery query) {
        List<CustomerSeasVo> list = customerPoolService.myPageList(query, SecurityUtils.getUserId());
        return success(new PageInfo<>(list));
    }

    @Log(title = "客户流转时间线", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('customer:seas:timeline')")
    @Operation(summary = "客户流转时间线(归属动作+外呼明细按时间混排,金蝶订单日志式)", method = "POST")
    @PostMapping("/timeline/{customerId}")
    public ResResult<PageInfo<CustomerTransferTimelineVo>> transferTimeline(@PathVariable("customerId") Long customerId,
                                                                            @RequestBody(required = false) CustomerSeasQuery query) {
        List<CustomerTransferTimelineVo> list = customerPoolService.getTransferTimeline(customerId,
                query == null ? null : query.getPageIndex(), query == null ? null : query.getPageSize());
        return success(new PageInfo<>(list));
    }

    @Log(title = "导入客户", businessType = BusinessTypeEnum.IMPORT)
    @PreAuthorize("@authz.hasPerm('customer:seas:import')")
    @Operation(summary = "导入客户", method = "POST")
    @PostMapping("/import/{templateId}")
    public ResResult importCustomer(@PathVariable("templateId") Long templateId, @RequestParam("file") MultipartFile file) {
        customerSeasService.importCustomer(templateId, file);
        return success();
    }

}
