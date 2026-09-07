// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.calltask.domain.CustomerCrowdEvent;
import com.hsc.calltask.domain.CustomerCrowdEventParam;
import com.hsc.calltask.domain.entity.CustomerCrowd;
import com.hsc.calltask.domain.entity.CustomerCrowdRel;
import com.hsc.calltask.domain.query.CustomerCrowdAddQuery;
import com.hsc.calltask.domain.query.CrowdCustomerQuery;
import com.hsc.calltask.domain.query.CustomerCrowdQuery;
import com.hsc.calltask.domain.vo.CrowdCustomerVo;
import com.hsc.calltask.domain.vo.CustomerCrowdVo;
import com.hsc.calltask.mapper.CustomerCrowdMapper;
import com.hsc.calltask.service.ICustomerCrowdRelService;
import com.hsc.calltask.service.ICustomerCrowdService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 客户人群管理表(CustomerCrowd)表服务实现类
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@RequiredArgsConstructor
@Service
public class CustomerCrowdServiceImpl extends BaseServiceImpl<CustomerCrowdMapper, CustomerCrowd> implements ICustomerCrowdService {

    private final ISysUserService sysUserService;
    private final ICustomerCrowdRelService customerCrowdRelService;
    private final ApplicationContext applicationContext;

    @Override
    public void add(CustomerCrowdAddQuery query) {
        boolean checkName = checkName(query.getName());
        if (checkName) {
            throw new CommonException("名称已存在");
        }
        CustomerCrowd customerCrowd = new CustomerCrowd();
        customerCrowd.setName(query.getName());
        customerCrowd.setRemark(query.getRemark());
        customerCrowd.setSwipe(JSONObject.toJSONString(query.getSwipe()));
        customerCrowd.setStatus(query.getStatus());
        customerCrowd.setType(query.getType());
        customerCrowd.setProgress(1);
        if(save(customerCrowd)){
            // 保存后立即触发全量计算，避免手动/自动人群停留在"待计算"无人触发
            publishCalcEvent(customerCrowd.getId());
        }
    }

    @Override
    public void edit(CustomerCrowdAddQuery query) {
        CustomerCrowd crowd = getById(query.getId());
        if (Objects.isNull(crowd)) {
            throw new CommonException("无效ID");
        }
        if (!Objects.equals(crowd.getName(), query.getName()) && checkName(query.getName())) {
            throw new CommonException("名称已存在");
        } else if (StringUtils.isNotBlank(query.getName())) {
            crowd.setName(query.getName());
        }
        if (StringUtils.isNotBlank(query.getRemark())) {
            crowd.setRemark(query.getRemark());
        }
        if (CollectionUtils.isNotEmpty(query.getSwipe())) {
            crowd.setSwipe(JSONObject.toJSONString(query.getSwipe()));
        }
        if (Objects.nonNull(query.getStatus())) {
            crowd.setStatus(query.getStatus());
        }
        if (Objects.nonNull(query.getType())) {
            crowd.setType(query.getType());
        }
        if(updateById(crowd)){
            // 编辑后重算，人群成员按新条件全量替换
            publishCalcEvent(crowd.getId());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(CustomerCrowdQuery query) {
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
        List<CustomerCrowd> list = ids.stream().map(id -> {
            CustomerCrowd customerCrowd = new CustomerCrowd();
            customerCrowd.setId(id);
            customerCrowd.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return customerCrowd;
        }).collect(Collectors.toList());
        if(updateBatchById(list)){
            // 级联软删人群客户关联，避免 rel 残留孤儿
            CustomerCrowdRel rel = new CustomerCrowdRel();
            rel.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            customerCrowdRelService.update(rel, new LambdaUpdateWrapper<CustomerCrowdRel>()
                    .in(CustomerCrowdRel::getCrowdId, ids)
                    .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        }
    }

    @Override
    public CustomerCrowdVo getDetail(Long id) {
        return this.baseMapper.getDetail(id);
    }

    @Override
    public List<CustomerCrowdVo> pageList(CustomerCrowdQuery query) {
        super.startPage(query.getPageIndex(), query.getPageSize());
        List<CustomerCrowdVo> list = getList(query);
        if (!CollectionUtil.isEmpty(list)) {
            sysUserService.decorate(list);
        }
        return list;
    }

    @Override
    public List<CustomerCrowdVo> getList(CustomerCrowdQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public List<CrowdCustomerVo> pageCustomerList(CrowdCustomerQuery query) {
        super.startPage(query.getPageIndex(), query.getPageSize());
        List<CrowdCustomerVo> list = getCustomerList(query);
        if (!CollectionUtil.isEmpty(list)) {
            sysUserService.decorate(list);
        }
        return list;
    }

    @Override
    public List<CrowdCustomerVo> getCustomerList(CrowdCustomerQuery query) {
        return this.baseMapper.pageCustomerList(query);
    }

    @Override
    public List<Long> getCustomerIdByCrowdId(Long crowdId) {
        List<CustomerCrowdRel> list = customerCrowdRelService.list(new LambdaQueryWrapper<CustomerCrowdRel>()
                .eq(CustomerCrowdRel::getCrowdId, crowdId)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .orderByDesc(CustomerCrowdRel::getId));
        if(CollectionUtil.isEmpty(list)){
            return Collections.emptyList();
        }
       return list.stream().map(CustomerCrowdRel::getCustomerId).collect(Collectors.toList());
    }

    /**
     * 触发该人群的全量计算事件
     *
     * @param crowdId 人群ID
     */
    private void publishCalcEvent(Long crowdId) {
        CustomerCrowdEventParam param = new CustomerCrowdEventParam();
        param.setCrowdId(crowdId);
        param.setEventType(3);
        applicationContext.publishEvent(new CustomerCrowdEvent(param));
    }

    /**
     * 检查名称
     *
     * @param name 名称
     * @return 是否重复
     */
    private boolean checkName(String name) {
        long count = count(new LambdaQueryWrapper<CustomerCrowd>().eq(CustomerCrowd::getName, name).eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        return count > 0;
    }
}

