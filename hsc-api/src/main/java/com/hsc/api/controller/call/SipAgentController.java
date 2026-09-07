// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.call;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.api.service.IDeskAgentService;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.system.domain.query.agent.SipAgentAddQuery;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.vo.agent.SipAgentConfigVo;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;
import com.hsc.system.service.ISipAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 坐席管理
 *
 * @author danmo
 * @date 2023年09月23日 10:41
 */
@Tag(name = "坐席管理")
@RestController
@RequestMapping("/system/v1/agent")
public class SipAgentController extends BaseController {

    @Autowired
    private ISipAgentService iSipAgentService;

    @Autowired
    private IDeskAgentService iDeskAgentService;

    @Log(title = "新增坐席", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:agent:add')")
    @Operation(summary = "新增坐席", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated SipAgentAddQuery query) {
        iSipAgentService.add(query);
        return ResResult.success();
    }

    @Log(title = "修改坐席", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:agent:edit')")
    @Operation(summary = "修改坐席", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated SipAgentAddQuery query) {
        query.setId(id);
        iSipAgentService.update(query);
        return ResResult.success();
    }

    @Log(title = "删除坐席", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:agent:delete')")
    @Operation(summary = "删除坐席", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody SipAgentQuery query) {
        iSipAgentService.delete(query);
        return ResResult.success();
    }

    @Log(title = "坐席详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:agent:get')")
    @Operation(summary = "坐席详情", method = "POST")
    @PostMapping("/get/{id}")
    public ResResult<SipAgentVo> getDetail(@PathVariable("id") Long id) {
        SipAgentVo detail = iSipAgentService.getDetail(id);
        return ResResult.success(detail);
    }

    @Log(title = "坐席列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:agent:page:list')")
    @Operation(summary = "坐席列表", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<SipAgentVo>> getPageList(@RequestBody SipAgentQuery query) {
        PageInfo<SipAgentVo> list = iSipAgentService.getPageList(query);
        return success(list);
    }

    @Operation(summary = "根据条件查询坐席", method = "POST")
    @PostMapping("/getInfoByQuery")
    public ResResult<List<SipAgentVo>> getInfoByQuery(@RequestBody SipAgentQuery query) {
        List<SipAgentVo> lfsSipAgentVo = iSipAgentService.getInfoByQuery(query);
        return success(lfsSipAgentVo);
    }

    @Operation(summary = "根据sip号码查询坐席", method = "GET")
    @GetMapping("/getInfoByAgent/{agentNum}")
    public ResResult<SipAgentVo> getInfoByAgent(@PathVariable("agentNum") String agentNum) {
        SipAgentVo lfsSipAgentVo = iSipAgentService.getInfoByAgent(agentNum);
        return ResResult.success(lfsSipAgentVo);
    }


    @Operation(summary = "坐席状态变更", method = "POST")
    @PostMapping("/update/status/{id}")
    public ResResult updateStatus(@PathVariable("id") Long id, @RequestBody SipAgentQuery query) {
        query.setId(id);
        if (query.getStatus() == null) {
            throw new CommonException("状态不能为空");
        }
        iSipAgentService.updateStatus(query.getId(), query.getStatus());
        return ResResult.success();
    }

    @Operation(summary = "获取当前登录用户的SIP配置", method = "GET")
    @GetMapping("/sipConfig")
    public ResResult<SipAgentConfigVo> getSipConfig() {
        Long userId = SecurityUtils.getUserId();
        SipAgentConfigVo config = iSipAgentService.getAgentSipConfig(userId);
        return ResResult.success(config);
    }

    @Operation(summary = "可绑定的SIP号码（排除已被其他坐席绑定的）", method = "GET")
    @GetMapping("/availableSipNumbers")
    public ResResult<List<SipSimpleVo>> availableSipNumbers(
            @RequestParam(required = false) Long excludeAgentId) {
        return ResResult.success(iSipAgentService.availableSipNumbers(excludeAgentId));
    }

    @Operation(summary = "座机签入(校验话机FS注册,通过写状态表空闲)", method = "POST")
    @PostMapping("/desk/signIn")
    public ResResult deskSignIn() {
        iDeskAgentService.signIn(SecurityUtils.getUserId());
        return ResResult.success();
    }

    @Operation(summary = "座机签出(置离线并移出状态表,不再分配任务客户)", method = "POST")
    @PostMapping("/desk/signOut")
    public ResResult deskSignOut() {
        iDeskAgentService.signOut(SecurityUtils.getUserId());
        return ResResult.success();
    }

    @Operation(summary = "当前坐席话机注册状态(座机点拨打前预检)", method = "POST")
    @PostMapping("/desk/registered")
    public ResResult<Boolean> deskRegistered() {
        return ResResult.success(iDeskAgentService.isPhoneRegistered(SecurityUtils.getUserId()));
    }
}
