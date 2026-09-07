// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.CallDisplay;
import com.hsc.system.domain.query.display.CallDisplayAddQuery;
import com.hsc.system.domain.query.display.CallDisplayQuery;
import com.hsc.system.domain.vo.display.CallDisplaySimpleVo;
import com.hsc.system.domain.vo.display.CallDisplayVo;
import com.hsc.system.mapper.CallDisplayMapper;
import com.hsc.system.service.ICallDisplayService;
import com.hsc.system.service.IPhoneLocationService;
import com.hsc.system.service.ISysUserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 号码管理(CallDisplay)表服务实现类
 *
 * @author danmo
 * @since 2023-10-23 10:45:58
 */
@AllArgsConstructor
@Service
public class CallDisplayServiceImpl extends BaseServiceImpl<CallDisplayMapper, CallDisplay> implements ICallDisplayService {

    private final ISysUserService sysUserService;
    private final IPhoneLocationService phoneLocationService;


    @Override
    public void add(CallDisplayAddQuery query) {
        checkPhone(query.getPhone());
        String location = phoneLocationService.getLocation(query.getPhone());
        CallDisplay callDisplay = new CallDisplay();
        BeanUtils.copyProperties(query, callDisplay);
        callDisplay.setArea(location);
        save(callDisplay);
    }

    @Override
    public void edit(CallDisplayAddQuery query) {
        CallDisplay display = getById(query.getId());
        if (Objects.isNull(display)) {
            throw new RuntimeException("无效ID");
        }
        // 号码变化时重算归属地；area 为空也重查(存量座机号解析能力上线前存的空值，
        // 编辑保存一次即刷新)
        if (!StringUtils.equals(query.getPhone(), display.getPhone())
                || StringUtils.isEmpty(display.getArea())) {
            String location = phoneLocationService.getLocation(query.getPhone());
            display.setArea(location);
            display.setPhone(query.getPhone());

        }
        checkPhone(query.getPhone(), query.getId());
        updateById(display);
    }

    @Override
    public CallDisplayVo getDetail(Long id) {
        CallDisplayVo callDisplayVo = new CallDisplayVo();
        CallDisplay detail = getById(id);
        BeanUtils.copyProperties(detail, callDisplayVo);
        return callDisplayVo;
    }

    @Override
    public void delete(CallDisplayQuery query) {
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
        List<CallDisplay> list = ids.stream().map(id -> {
            CallDisplay display = new CallDisplay();
            display.setId(id);
            display.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return display;
        }).collect(Collectors.toList());
        updateBatchById(list);
    }

    @Override
    public List<CallDisplayVo> getList(CallDisplayQuery query) {
        return baseMapper.getList(query);
    }

    @Override
    public void allocate(CallDisplayQuery query) {

    }

    @Override
    public List<CallDisplayVo> getPageList(CallDisplayQuery query) {
        startPage(query.getPageIndex(), query.getPageSize());
        List<CallDisplayVo> displayList = getList(query);
        if (CollectionUtil.isNotEmpty(displayList)) {
            sysUserService.decorate(displayList);
        }
        return displayList;
    }

    @Override
    public List<CallDisplaySimpleVo> selectSimpleList(CallDisplayQuery query) {
        List<CallDisplayVo> list = getList(query);
        if (CollectionUtil.isNotEmpty(list)) {
            return list.stream().map(displayVo -> {
                CallDisplaySimpleVo simpleVo = new CallDisplaySimpleVo();
                simpleVo.setDisplayId(displayVo.getId());
                simpleVo.setDisplayNumber(displayVo.getPhone());
                return simpleVo;
            }).toList();
        }
        return new ArrayList<>();
    }

    private void checkPhone(String phone) {
        checkPhone(phone, null);
    }

    /**
     * 校验号码唯一性。excludeId 不为空时排除该 id 自身（编辑场景：只改其他字段不改号码，
     * 不应命中自身记录而误报"号码已存在"）。
     */
    private void checkPhone(String phone, Long excludeId) {
        CallDisplayQuery callDisplayQuery = new CallDisplayQuery();
        callDisplayQuery.setPhone(phone);
        List<CallDisplayVo> list = getList(callDisplayQuery);
        boolean exists = CollectionUtil.isNotEmpty(list)
                && list.stream().anyMatch(vo -> !Objects.equals(vo.getId(), excludeId));
        if (exists) {
            throw new RuntimeException("号码已存在");
        }
    }
}

