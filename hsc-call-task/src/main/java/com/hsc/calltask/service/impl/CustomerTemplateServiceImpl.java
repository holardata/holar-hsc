// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.calltask.domain.entity.CustomerField;
import com.hsc.calltask.domain.entity.CustomerTemplate;
import com.hsc.calltask.domain.query.CustomerTemplateAddQuery;
import com.hsc.calltask.domain.query.CustomerTemplateFieldRelAddQuery;
import com.hsc.calltask.domain.query.CustomerTemplateQuery;
import com.hsc.calltask.domain.vo.CustomerTemplateVo;
import com.hsc.calltask.mapper.CustomerTemplateMapper;
import com.hsc.calltask.service.ICustomerFieldService;
import com.hsc.calltask.service.ICustomerTemplateFieldRelService;
import com.hsc.calltask.service.ICustomerTemplateService;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.handler.CommentWriteHandler;
import com.hsc.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 客户模板管理表(CustomerTemplate)表服务实现类
 *
 * @author danmo
 * @since 2025-06-30 11:35:44
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class CustomerTemplateServiceImpl extends BaseServiceImpl<CustomerTemplateMapper, CustomerTemplate> implements ICustomerTemplateService {

    private final ICustomerTemplateFieldRelService customerTemplateFieldRelService;
    private final ICustomerFieldService customerFieldService;
    private final ISysUserService sysUserService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CustomerTemplateAddQuery query) {
        Boolean isExist = checkName(query.getName());
        if (isExist) {
            throw new CommonException("名称已存在");
        }
        checkContainSystemFields(query.getFieldList());
        CustomerTemplate customerTemplate = new CustomerTemplate();
        customerTemplate.setName(query.getName());
        customerTemplate.setStatus(query.getStatus());
        if (save(customerTemplate)) {
            customerTemplateFieldRelService.saveByTemplateId(customerTemplate.getId(), query.getFieldList());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CustomerTemplateAddQuery query) {
        CustomerTemplate template = getById(query.getId());
        if (Objects.isNull(template)) {
            throw new CommonException("无效ID");
        }
        if (!StringUtils.equals(template.getName(), query.getName()) && checkName(query.getName())) {
            throw new CommonException("模板名称已存在");
        } else if (StringUtils.isNotBlank(query.getName())) {
            template.setName(query.getName());
        }
        if (Objects.nonNull(query.getStatus())) {
            template.setStatus(query.getStatus());
        }
        checkContainSystemFields(query.getFieldList());
        if (updateById(template)) {
            customerTemplateFieldRelService.updateByTemplateId(template.getId(), query.getFieldList());
        }
    }

    @Override
    public void delete(CustomerTemplateQuery query) {
        CustomerTemplate template = getById(query.getTemplateId());
        if (Objects.isNull(template)) {
            throw new CommonException("无效ID");
        }
        if (template.getStatus() == 1) {
            throw new CommonException("已启用的模板无法删除");
        }
        template.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        if (updateById(template)) {
            customerTemplateFieldRelService.deleteByTemplateId(template.getId());
        }
    }

    @Override
    public CustomerTemplateVo getDetail(Long id) {
        return this.baseMapper.getDetail(id);
    }

    @Override
    public PageInfo<CustomerTemplateVo> pageList(CustomerTemplateQuery query) {
        List<Long> ids = this.baseMapper.getIdsByQuery(query);
        if (CollectionUtil.isEmpty(ids)) {
            return new PageInfo<>(new LinkedList<>());
        }
        CustomerTemplateQuery customerTemplateQuery = new CustomerTemplateQuery();
        customerTemplateQuery.setTemplateIds(ids);
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<CustomerTemplateVo> list = getList(customerTemplateQuery);

        PageInfo<Long> pageIdInfo = new PageInfo<>(ids);
        PageInfo<CustomerTemplateVo> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(pageIdInfo.getTotal());
        pageInfo.setPageNum(pageIdInfo.getPageNum());
        pageInfo.setPageSize(pageIdInfo.getPageSize());
        if (CollectionUtil.isNotEmpty(pageInfo.getList())) {
            sysUserService.decorate(pageInfo.getList());
        }
        return pageInfo;
    }

    @Override
    public List<CustomerTemplateVo> getList(CustomerTemplateQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public void templateDownload(Long id, HttpServletResponse response) {
        CustomerTemplateVo template = getDetail(id);
        if (Objects.isNull(template)) {
            throw new CommonException("无效ID");
        }
        if(CollectionUtils.isEmpty(template.getFieldList())){
            throw new CommonException("请添加字段");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String name = URLEncoder.encode(template.getName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + name + ".xlsx");
        List<String> requiredList = Lists.newArrayList();
        List<List<String>> titles = template.getFieldList().stream().map(fieldInfo -> {
            List<String> title = Lists.newArrayList();
            if (Objects.equals(fieldInfo.getRequired(), 1)) {
                requiredList.add(fieldInfo.getFieldName());
            }
            title.add(fieldInfo.getFieldLabel() + "(" +fieldInfo.getFieldName() + ")");
            return title;
        }).toList();
        try {
            EasyExcel.write(response.getOutputStream())
                    .registerWriteHandler(new CommentWriteHandler(requiredList, "请填写必填项"))
                    .head(titles)
                    .sheet(template.getName())
                    .doWrite(Lists.newArrayList());
        } catch (Exception e) {
            throw new CommonException("导出失败");
        }
    }

    private Boolean checkName(String name) {
        long count = count(new LambdaQueryWrapper<CustomerTemplate>().eq(CustomerTemplate::getName, name).eq(CustomerTemplate::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        return count > 0;
    }

    /**
     * 模板必须包含全部系统预置字段(sys_type=0，客户名称/联系方式等锚点字段)，
     * 保证公海客户必有锚点数据(公海列表展示/外呼拨号取号依赖)
     */
    private void checkContainSystemFields(List<CustomerTemplateFieldRelAddQuery> fieldList) {
        List<CustomerField> systemFields = customerFieldService.listSystemFields();
        if (CollectionUtils.isEmpty(systemFields)) {
            return;
        }
        List<Long> selectedIds = CollectionUtils.isEmpty(fieldList) ? List.of()
                : fieldList.stream().map(CustomerTemplateFieldRelAddQuery::getFieldId).filter(Objects::nonNull).toList();
        List<String> missing = systemFields.stream()
                .filter(f -> !selectedIds.contains(f.getId()))
                .map(CustomerField::getFieldLabel)
                .toList();
        if (CollectionUtils.isNotEmpty(missing)) {
            throw new CommonException("模板必须包含系统字段：" + String.join("、", missing));
        }
    }
}

