package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.ChatPromptword;
import com.hsc.system.domain.query.ai.ChatPromptwordAddQuery;
import com.hsc.system.domain.query.ai.ChatPromptwordQuery;

/**
 * 智能体提示词(chat_promptword)表服务接口
 */
public interface IChatPromptwordService extends IBaseService<ChatPromptword> {

    /**
     * 按 title 精确查询提示词。
     *
     * @param title 提示词标题(如 "全文总结")
     * @return 提示词；不存在返回 null
     */
    ChatPromptword getByTitle(String title);

    /**
     * 分页查询提示词列表。
     */
    PageInfo<ChatPromptword> getPageList(ChatPromptwordQuery query);

    /**
     * 新增提示词(title 唯一，重复抛异常)。
     */
    void add(ChatPromptwordAddQuery query);

    /**
     * 修改提示词。
     */
    void update(ChatPromptwordAddQuery query);

    /**
     * 删除提示词(逻辑删除)。
     */
    void delete(ChatPromptwordQuery query);
}
