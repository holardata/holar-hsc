// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.calltask.domain.query.CustomerTemplateFieldRelAddQuery;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.calltask.mapper.CustomerTemplateFieldRelMapper;
import com.hsc.calltask.domain.entity.CustomerTemplateFieldRel;
import com.hsc.calltask.service.ICustomerTemplateFieldRelService;
import com.hsc.common.enums.DeleteStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 客户模板字段关联表(CustomerTemplateFieldRel)表服务实现类
 *
 * @author danmo
 * @since 2025-06-30 11:35:45
 */
@Service
public class CustomerTemplateFieldRelServiceImpl extends BaseServiceImpl<CustomerTemplateFieldRelMapper, CustomerTemplateFieldRel> implements ICustomerTemplateFieldRelService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveByTemplateId(Long templateId, List<CustomerTemplateFieldRelAddQuery> fieldList) {
        if(CollectionUtils.isEmpty(fieldList)){
            return;
        }
        List<CustomerTemplateFieldRel> list = fieldList.stream().map(item -> {
            CustomerTemplateFieldRel rel = new CustomerTemplateFieldRel();
            rel.setTemplateId(templateId);
            rel.setFieldId(item.getFieldId());
            rel.setHidden(item.getHidden());
            rel.setSort(item.getSort());
            return rel;
        }).toList();
        saveBatch(list);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateByTemplateId(Long templateId, List<CustomerTemplateFieldRelAddQuery> fieldList) {
        deleteByTemplateId(templateId);
        saveByTemplateId(templateId,fieldList);
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        deleteByTemplateId(List.of(templateId));
    }

    @Override
    public void deleteByTemplateId(List<Long> templateIds) {
        CustomerTemplateFieldRel rel = new CustomerTemplateFieldRel();
        rel.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        // templateIds 是集合，必须用 in；误用 eq 会把 List 整体当单值绑参(JDBC 序列化成 CollSer 字节流)，MySQL 报 Truncated incorrect INTEGER value
        update(rel,new LambdaUpdateWrapper<CustomerTemplateFieldRel>().in(CustomerTemplateFieldRel::getTemplateId,templateIds));
    }
}

