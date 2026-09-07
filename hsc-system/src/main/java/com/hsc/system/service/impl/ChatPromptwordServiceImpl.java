package com.hsc.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.ChatPromptword;
import com.hsc.system.domain.query.ai.ChatPromptwordAddQuery;
import com.hsc.system.domain.query.ai.ChatPromptwordQuery;
import com.hsc.system.mapper.ChatPromptwordMapper;
import com.hsc.system.service.IChatPromptwordService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 智能体提示词(chat_promptword)表服务实现
 */
@Service
public class ChatPromptwordServiceImpl extends BaseServiceImpl<ChatPromptwordMapper, ChatPromptword> implements IChatPromptwordService {

    @Override
    public ChatPromptword getByTitle(String title) {
        return getOne(new LambdaQueryWrapper<ChatPromptword>()
                .eq(ChatPromptword::getTitle, title)
                .last("limit 1"));
    }

    @Override
    public PageInfo<ChatPromptword> getPageList(ChatPromptwordQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<ChatPromptword> list = list(new LambdaQueryWrapper<ChatPromptword>()
                .like(StrUtil.isNotBlank(query.getTitle()), ChatPromptword::getTitle, query.getTitle())
                .orderByDesc(ChatPromptword::getId));
        return new PageInfo<>(list);
    }

    @Override
    public void add(ChatPromptwordAddQuery query) {
        if (checkTitle(query.getTitle(), null) != null) {
            throw new CommonException("提示词标题已存在");
        }
        ChatPromptword promptword = new ChatPromptword();
        BeanUtil.copyProperties(query, promptword);
        save(promptword);
    }

    @Override
    public void update(ChatPromptwordAddQuery query) {
        ChatPromptword existing = getById(query.getId());
        if (existing == null) {
            throw new CommonException("提示词不存在");
        }
        if (checkTitle(query.getTitle(), query.getId()) != null) {
            throw new CommonException("提示词标题已存在");
        }
        ChatPromptword promptword = new ChatPromptword();
        BeanUtil.copyProperties(query, promptword);
        updateById(promptword);
    }

    @Override
    public void delete(ChatPromptwordQuery query) {
        List<Long> ids = query.getIds();
        if ((ids == null || ids.isEmpty()) && query.getId() != null) {
            ids = Collections.singletonList(query.getId());
        }
        if (ids == null || ids.isEmpty()) {
            throw new CommonException("请选择要删除的提示词");
        }
        ChatPromptword del = new ChatPromptword();
        del.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        update(del, new LambdaQueryWrapper<ChatPromptword>().in(ChatPromptword::getId, ids));
    }

    private ChatPromptword checkTitle(String title, Long excludeId) {
        return getOne(new LambdaQueryWrapper<ChatPromptword>()
                .eq(ChatPromptword::getTitle, title)
                .ne(excludeId != null, ChatPromptword::getId, excludeId));
    }
}
