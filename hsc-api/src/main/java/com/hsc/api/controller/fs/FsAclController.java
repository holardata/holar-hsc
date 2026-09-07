// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.fs;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.esl.client.FsClient;
import com.hsc.system.domain.query.acl.FsAclAddQuery;
import com.hsc.system.domain.query.acl.FsAclNodeAddQuery;
import com.hsc.system.domain.query.acl.FsAclQuery;
import com.hsc.system.domain.vo.acl.FsAclVo;
import com.hsc.system.service.IFsAclService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author danmo
 * @date 2023年09月11日 13:37
 */
@Tag(name = "fs访问控制管理")
@RestController
@RequestMapping("/system/v1/acl")
public class FsAclController extends BaseController {

    @Autowired
    private IFsAclService iFsAclService;

    @Autowired
    private FsClient fsClient;

    @Log(title = "新增acl规则表", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:acl:table:add')")
    @Operation(summary = "新增acl规则表", method = "POST")
    @PostMapping("/list/add")
    public ResResult addList(@RequestBody @Validated FsAclAddQuery query) {
        iFsAclService.addList(query);
        fsClient.reloadAcl();
        return success();
    }

    @Log(title = "新增acl规则", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:acl:node:add')")
    @Operation(summary = "新增acl规则", method = "POST")
    @PostMapping("/node/add")
    public ResResult addNode(@RequestBody @Validated FsAclNodeAddQuery query) {
        iFsAclService.addNode(query);
        fsClient.reloadAcl();
        return success();
    }

    @Log(title = "修改acl规则表", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:acl:list:edit')")
    @Operation(summary = "修改acl规则表", method = "POST")
    @PostMapping("/list/edit/{id}")
    public ResResult editList(@PathVariable("id") Long id, @RequestBody @Validated FsAclAddQuery query) {
        query.setId(id);
        iFsAclService.editList(query);
        fsClient.reloadAcl();
        return success();
    }

    @Log(title = "修改acl规则", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:acl:node:edit')")
    @Operation(summary = "修改acl规则", method = "POST")
    @PostMapping("/node/edit/{id}")
    public ResResult editNode(@PathVariable("id") Long id, @RequestBody @Validated FsAclNodeAddQuery query) {
        query.setId(id);
        iFsAclService.editNode(query);
        fsClient.reloadAcl();
        return success();
    }

    @Log(title = "acl规则详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:acl:get')")
    @Operation(summary = "acl详情", method = "POST")
    @PostMapping("/get/{id}")
    public ResResult<FsAclVo> get(@PathVariable("id") Long id) {
        return success(iFsAclService.getDetail(id));
    }

    @Log(title = "删除acl", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:acl:delete')")
    @Operation(summary = "删除acl", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody FsAclQuery query) {
        iFsAclService.delete(query);
        fsClient.reloadAcl();
        return success();
    }

    @Log(title = "acl列表(分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:acl:page:list')")
    @Operation(summary = "acl列表(分页)", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<FsAclVo>> pageList(@RequestBody FsAclQuery query) {
        PageInfo<FsAclVo> list = iFsAclService.pageList(query);
        return success(list);
    }

    @Log(title = "acl列表(不分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:acl:list')")
    @Operation(summary = "acl列表(不分页)", method = "POST")
    @PostMapping("/list")
    public ResResult<List<FsAclVo>> getList(@RequestBody FsAclQuery query) {
        List<FsAclVo> list = iFsAclService.getList(query);
        return success(list);
    }
}
