// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.ko;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.subsriber.KoSubscriberAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberBatchAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberUpdateQuery;
import com.hsc.system.domain.vo.sip.FsRegVo;
import com.hsc.system.domain.vo.sip.KoSubscriberVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.service.IKoSubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author danmo
 * @date 2024年07月28日 13:37
 */
@Tag(name = "SIP号码管理")
@RestController
@RequestMapping("/system/v1/sip")
public class KoSubscriberController extends BaseController {

    @Autowired
    private IKoSubscriberService subscriberService;

    @Autowired
    private ISipRegService sipRegService;

    @Log(title = "新增SIP号码", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:add')")
    @Operation(summary = "新增SIP号码", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated KoSubscriberAddQuery query) {
        subscriberService.add(query);
        return success();
    }

    @Log(title = "批量新增SIP号码", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:batch:add')")
    @Operation(summary = "批量新增SIP号码", method = "POST")
    @PostMapping("/batch/add")
    public ResResult batchAdd(@RequestBody @Validated KoSubscriberBatchAddQuery query) {
        subscriberService.batchAdd(query);
        return success();
    }

    @Log(title = "修改SIP号码", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:subscriber:edit')")
    @Operation(summary = "修改SIP号码", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Integer id, @RequestBody @Validated KoSubscriberUpdateQuery query) {
        query.setId(id);
        subscriberService.edit(query);
        return success();
    }

    @Log(title = "SIP号码详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:get')")
    @Operation(summary = "SIP号码详情", method = "POST")
    @PostMapping("/get/{id}")
    public ResResult<KoSubscriber> get(@PathVariable("id") Integer id) {
        return ResResult.success(subscriberService.getDetail(id));
    }

    @Log(title = "删除SIP号码", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:subscriber:del')")
    @Operation(summary = "删除SIP号码", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody KoSubscriberQuery query) {
        subscriberService.delete(query);
        return success();
    }

    @Log(title = "SIP号码列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:page:list')")
    @Operation(summary = "SIP号码列表", method = "POST")
    @PostMapping("/list")
    public ResResult<PageInfo<KoSubscriberVo>> list(@RequestBody KoSubscriberQuery query) {
        List<KoSubscriberVo> list = subscriberService.getPageList(query);
        PageInfo<KoSubscriberVo> pageInfo = new PageInfo<>(list);
        return success(pageInfo);
    }

    @Log(title = "SIP号码下拉列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:select:list')")
    @Operation(summary = "SIP号码下拉列表", method = "POST")
    @PostMapping("/select/list")
    public ResResult<List<SipSimpleVo>> selectList(@RequestBody(required = false) KoSubscriberQuery query) {
        // terminalType 可选：传 1 只拉座机(号码路由"座机"目标值下拉用)；不传拉全部
        Integer terminalType = query == null ? null : query.getTerminalType();
        List<SipSimpleVo> list = subscriberService.selectList(terminalType);
        return success(list);
    }

    @Log(title = "SIP号码注册状态", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:subscriber:registrations')")
    @Operation(summary = "SIP号码注册状态(FS sofia reg,internal profile)", method = "POST")
    @PostMapping("/registrations")
    public ResResult<List<FsRegVo>> registrations(@RequestBody(required = false) KoSubscriberQuery query) {
        String username = query == null ? null : query.getUsername();
        return success(sipRegService.getList(username));
    }


}
