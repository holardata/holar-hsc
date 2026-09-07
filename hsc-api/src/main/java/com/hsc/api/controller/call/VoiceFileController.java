// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.api.controller.call;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.query.file.VoiceFileAddQuery;
import com.hsc.system.domain.query.file.VoiceFileQuery;
import com.hsc.system.domain.vo.file.VoiceFileVo;
import com.hsc.system.service.IVoiceFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author danmo
 * @date 2023-11-02 11:15
 **/
@Tag(name = "语音文件管理")
@RestController
@RequestMapping("call/v1/voice/file")
public class VoiceFileController extends BaseController {

    @Autowired
    private IVoiceFileService iVoiceFileService;

    @Log(title = "新增语音文件", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('call:voice:file:add')")
    @Operation(summary = "新增语音文件", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated VoiceFileAddQuery query) {
        iVoiceFileService.add(query);
        return ResResult.success();
    }

    @Log(title = "修改语音文件", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:voice:file:edit')")
    @Operation(summary = "修改语音文件", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated VoiceFileAddQuery query) {
        query.setId(id);
        iVoiceFileService.edit(query);
        return ResResult.success();
    }

    /**
     * 重新生成（音频偶发问题时原样重合）：删除旧音频文件并按表单值强制重新合成。
     * 权限复用编辑（同为修改记录的音频产物，不新增 F 按钮）。
     */
    @Log(title = "重新生成语音文件", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('call:voice:file:edit')")
    @Operation(summary = "重新生成语音文件(删除旧音频强制重合)", method = "POST")
    @PostMapping("/regenerate/{id}")
    public ResResult regenerate(@PathVariable("id") Long id, @RequestBody @Validated VoiceFileAddQuery query) {
        query.setId(id);
        iVoiceFileService.regenerate(query);
        return ResResult.success();
    }

    @Log(title = "删除语音文件", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('call:voice:file:delete')")
    @Operation(summary = "删除语音文件", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody VoiceFileQuery query) {
        iVoiceFileService.delete(query);
        return ResResult.success();
    }

    @Log(title = "语音文件详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:voice:file:get')")
    @Operation(summary = "语音文件详情", method = "POST")
    @PostMapping("/get/{id}")
    public ResResult<VoiceFileVo> getDetail(@PathVariable("id") Long id) {
        VoiceFileVo detail = iVoiceFileService.getDetail(id);
        return ResResult.success(detail);
    }

    @Log(title = "语音文件列表(分页)", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('call:voice:file:page:list')")
    @Operation(summary = "语音文件列表(分页)", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<VoiceFileVo>> pageList(@RequestBody VoiceFileQuery query) {
        List<VoiceFileVo> list = iVoiceFileService.getPageList(query);
        PageInfo<VoiceFileVo> pageInfo = new PageInfo<>(list);
        return success(pageInfo);
    }

    @Operation(summary = "语音文件列表", method = "POST")
    @PostMapping("/list")
    public ResResult<List<VoiceFileVo>> getList(@RequestBody VoiceFileQuery query) {
        List<VoiceFileVo> list = iVoiceFileService.getList(query);
        return success(list);
    }
}
