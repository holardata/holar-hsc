package com.hsc.api.controller.ai;

import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.Log;
import com.hsc.common.base.BaseController;
import com.hsc.common.base.ResResult;
import com.hsc.common.enums.BusinessTypeEnum;
import com.hsc.system.domain.entity.ChatPromptword;
import com.hsc.system.domain.query.ai.ChatPromptwordAddQuery;
import com.hsc.system.domain.query.ai.ChatPromptwordQuery;
import com.hsc.system.service.IChatPromptwordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 智能体提示词管理（chat_promptword）：各业务场景提示词的增删改查，摘要业务按 title 取用。
 */
@Tag(name = "智能体提示词管理")
@RestController
@RequestMapping("/system/v1/promptword")
public class ChatPromptwordController extends BaseController {

    @Autowired
    private IChatPromptwordService iChatPromptwordService;

    @Log(title = "提示词列表", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:promptword:page:list')")
    @Operation(summary = "提示词分页列表", method = "POST")
    @PostMapping("/page/list")
    public ResResult<PageInfo<ChatPromptword>> getPageList(@RequestBody ChatPromptwordQuery query) {
        return success(iChatPromptwordService.getPageList(query));
    }

    @Log(title = "提示词详情", businessType = BusinessTypeEnum.SELECT)
    @PreAuthorize("@authz.hasPerm('system:promptword:get')")
    @Operation(summary = "提示词详情", method = "GET")
    @GetMapping("/get/{id}")
    public ResResult<ChatPromptword> getDetail(@PathVariable("id") Long id) {
        return success(iChatPromptwordService.getById(id));
    }

    @Log(title = "新增提示词", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("@authz.hasPerm('system:promptword:add')")
    @Operation(summary = "新增提示词", method = "POST")
    @PostMapping("/add")
    public ResResult add(@RequestBody @Validated ChatPromptwordAddQuery query) {
        iChatPromptwordService.add(query);
        return success();
    }

    @Log(title = "修改提示词", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("@authz.hasPerm('system:promptword:edit')")
    @Operation(summary = "修改提示词", method = "POST")
    @PostMapping("/edit/{id}")
    public ResResult edit(@PathVariable("id") Long id, @RequestBody @Validated ChatPromptwordAddQuery query) {
        query.setId(id);
        iChatPromptwordService.update(query);
        return success();
    }

    @Log(title = "删除提示词", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@authz.hasPerm('system:promptword:delete')")
    @Operation(summary = "删除提示词", method = "POST")
    @PostMapping("/delete")
    public ResResult delete(@RequestBody ChatPromptwordQuery query) {
        iChatPromptwordService.delete(query);
        return success();
    }
}
