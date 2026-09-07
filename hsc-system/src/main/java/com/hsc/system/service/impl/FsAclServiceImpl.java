// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.github.pagehelper.PageInfo;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.FsAcl;
import com.hsc.system.domain.query.acl.FsAclAddQuery;
import com.hsc.system.domain.query.acl.FsAclNodeAddQuery;
import com.hsc.system.domain.query.acl.FsAclQuery;
import com.hsc.system.domain.vo.acl.FsAclVo;
import com.hsc.system.mapper.FsAclMapper;
import com.hsc.system.service.IFsAclService;
import com.hsc.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * fs访问控制表(FsAcl)
 *
 * @author danmo
 * @date 2023-09-13 13:53:45
 */
@Service
public class FsAclServiceImpl extends BaseServiceImpl<FsAclMapper, FsAcl> implements IFsAclService {

    /**
     * 系统预置 list 名(FS xml apply-xxx-acl 引用,list 锁死:不可增删/不可改名)。
     * event_socket.auto 还是 FsAclXmlCurlHandler 硬编码触发条件(注入后端 IP 兜底)。
     * internal-trust/external-trust 为双 profile 分面信任名单(sip-profile-normalization);
     * 旧 domains 已停用(dml del_flag=1),移出保护集。
     */
    private static final Set<String> PRESET_LIST_NAMES = Set.of("candidate-allow", "internal-trust", "external-trust", "event_socket.auto");

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public void addList(FsAclAddQuery query) {
        // list 锁死:访问控制 list 为系统预置(对应 FS xml apply-xxx-acl 引用),不支持业务新增。
        // 要加新 ACL 用途:先 FS xml 加 apply-xxx-acl=新key 引用,再 changeset 预置该 list。
        throw new CommonException("访问控制列表为系统预置,不支持新增");
    }

    @Override
    public void addNode(FsAclNodeAddQuery query) {
        if (Objects.isNull(query.getListId())) {
            throw new CommonException("规则表ID不能为空");
        }
        FsAcl fsAcl = new FsAcl();
        fsAcl.setName(query.getName());
        fsAcl.setDefaultType(query.getDefaultType());
        fsAcl.setListId(query.getListId());
        fsAcl.setCidr(query.getCidr());
        fsAcl.setNodeType(query.getNodeType());
        fsAcl.setDomain(query.getDomain());
        fsAcl.setRemark(query.getRemark());
        save(fsAcl);
    }

    @Override
    public void editList(FsAclAddQuery query) {
        FsAcl fsAcl = getById(query.getId());
        if (Objects.isNull(fsAcl)) {
            throw new CommonException("无效ID");
        }
        // name 锁死(FS apply-xxx-acl 引用 key + event_socket.auto 硬编码触发条件):
        // 后端永不 set name(MyBatis-Plus updateById 只更新非 null 字段,name 未 set 即不变);
        // 前端 name disabled。请求即使带 name 也忽略,只允许改 defaultType/remark/description。
        FsAcl updateFsAcl = new FsAcl();
        updateFsAcl.setId(query.getId());
        updateFsAcl.setDefaultType(query.getDefaultType());
        updateFsAcl.setRemark(query.getRemark());
        updateFsAcl.setDescription(query.getDescription());
        updateById(updateFsAcl);
    }

    @Override
    public void editNode(FsAclNodeAddQuery query) {
        FsAcl fsAcl = getById(query.getId());
        if (Objects.isNull(fsAcl)) {
            throw new CommonException("无效ID");
        }
        FsAcl updateFsAcl = new FsAcl();
        updateFsAcl.setId(query.getId());
        updateFsAcl.setListId(query.getListId());
        updateFsAcl.setName(query.getName());
        updateFsAcl.setDefaultType(query.getDefaultType());
        updateFsAcl.setCidr(query.getCidr());
        updateFsAcl.setNodeType(query.getNodeType());
        updateFsAcl.setDomain(query.getDomain());
        updateFsAcl.setRemark(query.getRemark());
        updateById(updateFsAcl);
    }

    @Override
    public FsAclVo getDetail(Long id) {
        FsAclQuery query = new FsAclQuery();
        query.setIds(Collections.singletonList(id));
        List<FsAclVo> list = getList(query);
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        FsAclVo vo = list.get(0);
        stripEmptyNodes(vo);
        return vo;
    }

    /**
     * 剔除 left join 产生的空 node 行(node id 为空)。pageList 与 getDetail 共用。
     */
    private void stripEmptyNodes(FsAclVo vo) {
        if (vo.getNodeList() == null) {
            return;
        }
        vo.setNodeList(
                vo.getNodeList().stream().filter(item -> Objects.nonNull(item.getId())).toList()
        );
    }

    @Override
    public void delete(FsAclQuery query) {
        List<Long> ids = new LinkedList<>();
        if (Objects.nonNull(query.getId())) {
            ids.add(query.getId());
        }
        if (CollectionUtil.isNotEmpty(query.getIds())) {
            ids.addAll(query.getIds());
        }
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        // 预置 list 禁删(candidate-allow/domains/event_socket.auto 被 FS xml 引用)
        List<FsAcl> presetLists = this.lambdaQuery()
                .select(FsAcl::getId, FsAcl::getName)
                .in(FsAcl::getId, ids)
                .eq(FsAcl::getListId, 0)
                .in(FsAcl::getName, PRESET_LIST_NAMES)
                .list();
        if (CollectionUtil.isNotEmpty(presetLists)) {
            throw new CommonException("系统预置列表[" + presetLists.get(0).getName() + "]不可删除");
        }
        // 校验:删 list 头前,其下不能有 node,必须先删完 node
        long nodeCount = this.lambdaQuery()
                .in(FsAcl::getListId, ids)
                .eq(FsAcl::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .count();
        if (nodeCount > 0) {
            throw new CommonException("该列表下存在节点,请先删除节点后再删除列表");
        }
        List<FsAcl> list = ids.stream().map(id -> {
            FsAcl fsAcl = new FsAcl();
            fsAcl.setId(id);
            fsAcl.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return fsAcl;
        }).collect(Collectors.toList());
        updateBatchById(list);
    }

    @Override
    public PageInfo<FsAclVo> pageList(FsAclQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<Long> ids = this.baseMapper.getIdsByQuery(query);
        if (CollectionUtil.isEmpty(ids)) {
            return new PageInfo<>(new LinkedList<>());
        }
        query.setIds(ids);
        List<FsAclVo> list = getList(query);
        if(CollectionUtil.isNotEmpty(list)){
            sysUserService.decorate(list);
            for (FsAclVo fsAclVo : list) {
                stripEmptyNodes(fsAclVo);
                sysUserService.decorate(fsAclVo.getNodeList());
            }
        }
        PageInfo<Long> pageIdInfo = new PageInfo<>(ids);
        PageInfo<FsAclVo> pageInfo = new PageInfo<>(list);
        pageInfo.setTotal(pageIdInfo.getTotal());
        pageInfo.setPageNum(pageIdInfo.getPageNum());
        pageInfo.setPageSize(pageIdInfo.getPageSize());
        return pageInfo;
    }

    @Override
    public List<FsAclVo> getList(FsAclQuery query) {
        return this.baseMapper.getList(query);
    }

}
