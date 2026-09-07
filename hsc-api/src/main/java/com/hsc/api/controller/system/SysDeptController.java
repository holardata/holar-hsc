package com.hsc.api.controller.system;

import cn.hutool.core.lang.tree.Tree;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.SysDept;
import com.hsc.system.service.ISysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理（dept-management 1F，平迁 rb Sys01DeptController，适配 hsc sys_dept 表）。
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/v1/dept")
public class SysDeptController extends BaseController {

    @Autowired
    private ISysDeptService iSysDeptService;

    @Log(title = "新增部门", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:dept:add')")
    @Operation(summary = "新增部门", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody SysDept dept) {
        iSysDeptService.add(dept);
        return success();
    }

    @Log(title = "修改部门", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dept:edit')")
    @Operation(summary = "修改部门", method = "POST")
    @PostMapping("/edit")
    public ResResult edit(@RequestBody SysDept dept) {
        iSysDeptService.edit(dept);
        return success();
    }

    @Log(title = "删除部门", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:dept:delete')")
    @Operation(summary = "删除部门", method = "POST")
    @PostMapping("/delete/{deptId}")
    public ResResult delete(@PathVariable("deptId") Long deptId) {
        iSysDeptService.delete(deptId);
        return success();
    }

    @Log(title = "部门详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:dept:get')")
    @Operation(summary = "部门详情", method = "GET")
    @GetMapping("/get/{deptId}")
    public ResResult<SysDept> getDetail(@PathVariable("deptId") Long deptId) {
        return success(iSysDeptService.getDetail(deptId));
    }

    @Log(title = "部门树", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:dept:tree')")
    @Operation(summary = "部门树", method = "GET")
    @GetMapping("/tree")
    public ResResult<List<Tree<Long>>> tree(
            @RequestParam(value = "deptName", required = false) String deptName) {
        return success(iSysDeptService.treeList(deptName));
    }

    @Log(title = "部门后代", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:dept:tree')")
    @Operation(summary = "部门后代列表", method = "GET")
    @GetMapping("/descendants/{deptId}")
    public ResResult<List<SysDept>> descendants(@PathVariable("deptId") Long deptId) {
        return success(iSysDeptService.getDescendants(deptId));
    }

    @Log(title = "停用部门", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dept:edit')")
    @Operation(summary = "停用部门", method = "POST")
    @PostMapping("/lock/{deptId}")
    public ResResult lock(@PathVariable("deptId") Long deptId) {
        iSysDeptService.lockDept(deptId);
        return success();
    }

    @Log(title = "启用部门", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:dept:edit')")
    @Operation(summary = "启用部门", method = "POST")
    @PostMapping("/unlock/{deptId}")
    public ResResult unlock(@PathVariable("deptId") Long deptId) {
        iSysDeptService.unlockDept(deptId);
        return success();
    }
}
