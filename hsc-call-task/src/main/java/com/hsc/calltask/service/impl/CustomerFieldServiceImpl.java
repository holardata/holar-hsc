// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.calltask.domain.entity.CustomerField;
import com.hsc.calltask.domain.query.CustomerFieldAddQuery;
import com.hsc.calltask.domain.query.CustomerFieldQuery;
import com.hsc.calltask.domain.vo.CustomerFieldVo;
import com.hsc.calltask.mapper.CustomerFieldMapper;
import com.hsc.calltask.service.ICustomerFieldService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 客户字段管理表(CustomerField)表服务实现类
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@RequiredArgsConstructor
@Service
public class CustomerFieldServiceImpl extends BaseServiceImpl<CustomerFieldMapper, CustomerField> implements ICustomerFieldService {

    private final ISysUserService sysUserService;

    @Override
    public void add(CustomerFieldAddQuery query) {
        checkFieldNameConflict(query.getFieldName(), null);
        CustomerField customerField = new CustomerField();
        BeanUtils.copyProperties(query, customerField);
        // 系统字段(sys_type=0)只能来自预置数据，页面/API 新增一律自定义字段
        customerField.setSysType(1);
        // options 列是 MySQL JSON 类型，空串不是合法 JSON("The document is empty")，置 null 落库默认值
        if (StringUtils.isBlank(customerField.getOptions())) {
            customerField.setOptions(null);
        }
        save(customerField);
    }


    @Override
    public void edit(CustomerFieldAddQuery query) {
        CustomerField field = getById(query.getId());
        if (Objects.isNull(field)) {
            throw new CommonException("无效ID");
        }
        if (Objects.equals(field.getSysType(), 0)) {
            throw new CommonException("系统字段不允许修改");
        }
        checkFieldNameConflict(query.getFieldName(), query.getId());
        if (StringUtils.isNotBlank(query.getFieldName())) {
            field.setFieldName(query.getFieldName());
        }
        if (StringUtils.isNotBlank(query.getFieldLabel())) {
            field.setFieldLabel(query.getFieldLabel());
        }
        if (Objects.nonNull(query.getFieldType())) {
            field.setFieldType(query.getFieldType());
        }
        if (Objects.nonNull(query.getRequired())) {
            field.setRequired(query.getRequired());
        }
        if (StringUtils.isNotBlank(query.getOptions())) {
            field.setOptions(query.getOptions());
        }
        if (Objects.nonNull(query.getStatus())) {
            field.setStatus(query.getStatus());
        }
        updateById(field);
    }

    @Override
    public CustomerFieldVo getDetail(Long id) {
        CustomerField field = getById(id);
        if (Objects.isNull(field)) {
            throw new CommonException("无效ID");
        }
        CustomerFieldVo fieldVo = new CustomerFieldVo();
        BeanUtils.copyProperties(field, fieldVo);
        return fieldVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(CustomerFieldQuery query) {
        List<Long> ids = new LinkedList<>();
        if (Objects.nonNull(query.getId())) {
            ids.add(query.getId());
        }
        if (CollectionUtil.isNotEmpty(query.getIdList())) {
            ids.addAll(query.getIdList());
        }
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        List<Long> systemFieldIds = listSystemFields().stream().map(CustomerField::getId).toList();
        if (ids.stream().anyMatch(systemFieldIds::contains)) {
            throw new CommonException("系统字段不允许删除");
        }
        // 物理删除：配置表无已删数据的展示/恢复入口，逻辑删除的行只会占 field_name
        // 唯一索引(idx_unique_name)挡住同名字段重建
        removeByIds(ids);
    }

    @Override
    public List<CustomerFieldVo> pageList(CustomerFieldQuery query) {
        super.startPage(query.getPageIndex(), query.getPageSize());
        List<CustomerFieldVo> customerFields = getList(query);
        sysUserService.decorate(customerFields);
        return customerFields;
    }

    @Override
    public List<CustomerFieldVo> getList(CustomerFieldQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public List<CustomerField> listSystemFields() {
        return list(new LambdaQueryWrapper<CustomerField>()
                .eq(CustomerField::getSysType, 0)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
    }

    /**
     * 字段标识冲突校验：与系统字段标识撞名或与其他字段重名均拒绝
     *
     * @param fieldName 待校验标识
     * @param excludeId 编辑时排除自身ID，新增传 null
     */
    private void checkFieldNameConflict(String fieldName, Long excludeId) {
        if (StringUtils.isBlank(fieldName)) {
            return;
        }
        if (listSystemFields().stream().anyMatch(f -> StringUtils.equals(f.getFieldName(), fieldName))) {
            throw new CommonException("字段标识与系统字段冲突，请更换");
        }
        LambdaQueryWrapper<CustomerField> wrapper = new LambdaQueryWrapper<CustomerField>()
                .eq(CustomerField::getFieldName, fieldName)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex());
        if (Objects.nonNull(excludeId)) {
            wrapper.ne(CustomerField::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new CommonException("字段名称已存在");
        }
    }
}

