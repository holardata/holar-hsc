// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service;

import com.github.pagehelper.PageInfo;
import com.hsc.calltask.domain.query.CustomerTemplateAddQuery;
import com.hsc.calltask.domain.query.CustomerTemplateQuery;
import com.hsc.calltask.domain.vo.CustomerTemplateVo;
import com.hsc.common.base.IBaseService;
import com.hsc.calltask.domain.entity.CustomerTemplate;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 客户模板管理表(CustomerTemplate)表服务接口
 *
 * @author danmo
 * @since 2025-06-30 11:35:44
 */
public interface ICustomerTemplateService extends IBaseService<CustomerTemplate> {

    /**
     * 新增客户模板
     * @param query 新增参数
     */
    void add(CustomerTemplateAddQuery query);

    /**
     * 修改客户模板
     * @param query 修改参数
     */
    void edit(CustomerTemplateAddQuery query);

    /**
     * 删除客户模板
     * @param query 删除参数
     */
    void delete(CustomerTemplateQuery query);

    /**
     * 获取客户模板详情
     * @param id 客户模板ID
     * @return 客户模板详情
     */
    CustomerTemplateVo getDetail(Long id);

    /**
     * 获取客户模板列表(分页)
     * @param query 查询参数
     * @return 客户模板列表
     */
    PageInfo<CustomerTemplateVo> pageList(CustomerTemplateQuery query);

    /**
     * 获取客户模板列表(不分页)
     * @param query 查询参数
     * @return 客户模板列表
     */
    List<CustomerTemplateVo> getList(CustomerTemplateQuery query);

    /**
     * 客户模板下载
     * @param id 客户模板ID
     * @param response 响应
     */
    void templateDownload(Long id, HttpServletResponse response);
}

