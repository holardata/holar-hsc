package com.hsc.calltask.service;

import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.query.CustomerSeasAddQuery;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.common.base.IBaseService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 客户公海表(CustomerSeas)表服务接口
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
public interface ICustomerSeasService extends IBaseService<CustomerSeas> {

    /**
     * 新增客户公海
     *
     * @param query 新增参数
     */
    void add(CustomerSeasAddQuery query);

    /**
     * 修改客户公海
     *
     * @param query 修改参数
     */
    void edit(CustomerSeasAddQuery query);

    /**
     * 获取客户详情
     *
     * @param id 客户ID
     */
    CustomerSeasVo getDetail(Long id);

    /**
     * 删除客户
     *
     * @param query 删除参数
     */
    void delete(CustomerSeasQuery query);

    /**
     * 获取客户列表
     *
     * @param query 查询参数
     */
    List<CustomerSeasVo> pageList(CustomerSeasQuery query);

    /**
     * 获取客户列表
     *
     * @param query 获取参数
     */
    List<CustomerSeasVo> getList(CustomerSeasQuery query);

    /**
     * 导入客户
     * @param templateId 模板ID
     * @param file 文件
     */
    void importCustomer(Long templateId, MultipartFile file);
}

