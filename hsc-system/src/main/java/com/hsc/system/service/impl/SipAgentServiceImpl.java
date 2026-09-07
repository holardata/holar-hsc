// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.entity.SipAgent;
import com.hsc.system.domain.query.agent.SipAgentAddQuery;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.vo.agent.SipAgentConfigVo;
import com.hsc.system.domain.vo.agent.SipAgentStatusVo;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;
import com.hsc.system.mapper.SipAgentMapper;
import com.hsc.system.service.IKoSubscriberService;
import com.hsc.system.service.ISipAgentService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 坐席管理表(SipAgent)
 *
 * @author danmo
 * @date 2023-09-26 11:08:58
 */
@AllArgsConstructor
@Service
public class SipAgentServiceImpl extends BaseServiceImpl<SipAgentMapper, SipAgent> implements ISipAgentService {

    private final RedisService redisService;
    private final IKoSubscriberService koSubscriberService;

    @Override
    public void add(SipAgentAddQuery query) {
        // 一坐席一号：保存前校验该 SIP 号码未被其他坐席绑定（防并发重复绑定）
        checkAgentNumberUnique(query.getAgentNumber(), null);
        SipAgent lfsAgent = new SipAgent();
        lfsAgent.setName(query.getName());
        lfsAgent.setStatus(query.getStatus());
        lfsAgent.setUserId(query.getUserId());
        lfsAgent.setAgentNumber(query.getAgentNumber());
        save(lfsAgent);
    }

    @Override
    public void update(SipAgentAddQuery query) {
        SipAgent sipAgent = getById(query.getId());
        if (Objects.isNull(sipAgent)) {
            return;
        }
        if (!Objects.equals(sipAgent.getAgentNumber(), query.getAgentNumber())){
            // 一坐席一号：号码变更时校验新号未被其他坐席绑定
            checkAgentNumberUnique(query.getAgentNumber(), query.getId());
            sipAgent.setAgentNumber(query.getAgentNumber());
        }
        if (!Objects.equals(sipAgent.getUserId(), query.getUserId())){
            sipAgent.setUserId(query.getUserId());
        }
        if (!Objects.equals(sipAgent.getName(), query.getName())){
            sipAgent.setName(query.getName());
        }
        if (!Objects.equals(sipAgent.getStatus(), query.getStatus())){
            sipAgent.setStatus(query.getStatus());
        }
        if(updateById(sipAgent)){
            redisService.delCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, sipAgent.getId().toString());
        };
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(SipAgentQuery query) {
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
        List<SipAgent> list = ids.stream().map(id -> {
            SipAgent agent = new SipAgent();
            agent.setId(id);
            agent.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return agent;
        }).collect(Collectors.toList());
        if(updateBatchById(list)){
            String[] idArray = ids.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redisService.delCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, idArray);
        }
    }

    @Override
    public SipAgentVo getDetail(Long id) {
        return this.baseMapper.getDetail(id);
    }

    @Override
    public PageInfo<SipAgentVo> getPageList(SipAgentQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<SipAgentVo> resList = getInfoByQuery(query);
        return new PageInfo<>(resList);
    }

    @Override
    public List<SipAgentVo> getInfoByQuery(SipAgentQuery query) {
        return this.baseMapper.getInfoByQuery(query);
    }

    @Override
    public Long getAgentIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SipAgentQuery query = new SipAgentQuery();
        query.setUserId(userId);
        List<SipAgentVo> agents = getInfoByQuery(query);
        return CollectionUtil.isEmpty(agents) ? null : agents.get(0).getId();
    }

    @Override
    public SipAgentVo getInfoByAgent(String agentNum) {
        return this.baseMapper.getInfoByAgent(agentNum);
    }


    @Override
    public Boolean updateStatus(Long id, Integer status) {
        SipAgent agent = new SipAgent();
        agent.setId(id);
        agent.setStatus(status);
        return updateById(agent);
    }

    @Override
    public List<SipAgentStatusVo> getAgentStatusList(List<Long> agentIds) {
        Map<String, SipAgentStatusVo> cacheMap = redisService.getCacheMap(CacheConstants.AGENT_CURRENT_STATUS_KEY);
        if (CollectionUtil.isNotEmpty(cacheMap) && CollectionUtil.isNotEmpty(agentIds)) {
            return cacheMap.values().stream().filter(agent -> agentIds.contains(agent.getId())).toList();
        }if(CollectionUtil.isNotEmpty(cacheMap) && CollectionUtil.isEmpty(agentIds)){
            return cacheMap.values().stream().toList();
        }
        return List.of();
    }

    @Override
    public void updateOnlineStatus(Long id, Integer onlineStatus, Long timestamp) {
        SipAgent agent = new SipAgent();
        agent.setId(id);
        agent.setOnlineStatus(onlineStatus);
        if(updateById(agent)){
            Boolean hasKey = redisService.getCacheMapHasKey(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(id));
            if(!hasKey){
                SipAgentVo detail = getDetail(id);
                SipAgentStatusVo agentStatus = new SipAgentStatusVo();
                BeanUtils.copyProperties(detail, agentStatus);
                agentStatus.setOnlineStatus(onlineStatus);
                agentStatus.setStatus(onlineStatus);
                agentStatus.setStatusTime(timestamp);
                redisService.setCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY,String.valueOf(id),agentStatus);
            }else {
                SipAgentStatusVo agentStatus = redisService.getCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(id));
                agentStatus.setOnlineStatus(onlineStatus);
                agentStatus.setStatus(onlineStatus);
                agentStatus.setStatusTime(timestamp);
                redisService.setCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY,String.valueOf(id),agentStatus);
            }
        }
    }

    @Override
    public SipAgentConfigVo getAgentSipConfig(Long userId) {
        SipAgentQuery query = new SipAgentQuery();
        query.setUserId(userId);
        List<SipAgentVo> agents = getInfoByQuery(query);
        if (CollectionUtil.isEmpty(agents)) {
            return null;
        }
        SipAgentVo agent = agents.get(0);
        SipAgentConfigVo config = new SipAgentConfigVo();
        config.setAgentId(agent.getId());
        // 签入展示名：坐席名-员工姓名（员工未绑定或昵称为空时退回坐席名）
        config.setAgentName(StrUtil.isBlank(agent.getNickName())
                ? agent.getName()
                : agent.getName() + "-" + agent.getNickName());
        config.setAgentNumber(agent.getAgentNumber());
        KoSubscriber subscriber = koSubscriberService.getByUserName(agent.getAgentNumber());
        if (subscriber != null) {
            config.setTerminalType(subscriber.getTerminalType());
            config.setDomain(subscriber.getDomain());
            // 座机不返回 SIP 密码：座机坐席浏览器不注册 JsSIP，防止误用座机号签入把在线座机踢下线（SIP 同号互踢）
            if (!Objects.equals(subscriber.getTerminalType(), 1)) {
                config.setPassword(subscriber.getPassword());
            }
        }
        return config;
    }

    /**
     * 校验 SIP 号码未被其他坐席绑定（一坐席一号约束）。
     * @param agentNumber 待绑定的 SIP 号码
     * @param excludeId 编辑场景排除当前坐席自身 id（新增传 null）
     */
    private void checkAgentNumberUnique(String agentNumber, Long excludeId) {
        long count = count(new LambdaQueryWrapper<SipAgent>()
                .eq(SipAgent::getAgentNumber, agentNumber)
                .eq(SipAgent::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .ne(excludeId != null, SipAgent::getId, excludeId));
        if (count > 0) {
            throw new CommonException("该 SIP 号码已被其他坐席绑定");
        }
    }

    @Override
    public List<SipSimpleVo> availableSipNumbers(Long excludeAgentId) {
        // 1. 启用中的全部号码（软电话+座机均可绑坐席：座机绑定后用于通话 ws 推送定位/AI 工作台）
        List<SipSimpleVo> all = koSubscriberService.selectList(null);
        if (CollectionUtil.isEmpty(all)) {
            return List.of();
        }
        // 2. 已被其他坐席绑定的号码（排除当前编辑坐席自身）
        List<SipAgent> boundAgents = list(new LambdaQueryWrapper<SipAgent>()
                .select(SipAgent::getAgentNumber)
                .eq(SipAgent::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .ne(excludeAgentId != null, SipAgent::getId, excludeAgentId));
        Set<String> boundNumbers = CollectionUtil.isEmpty(boundAgents)
                ? new HashSet<>()
                : boundAgents.stream()
                        .map(SipAgent::getAgentNumber)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        // 3. 返回未被绑定的
        return all.stream()
                .filter(s -> !boundNumbers.contains(s.getSipName()))
                .collect(Collectors.toList());
    }

    @Override
    public String buildNameSnapshot(Long agentId, String fallback) {
        if (agentId == null) {
            return fallback;
        }
        try {
            SipAgentVo agent = getDetail(agentId);
            if (agent == null || StrUtil.isBlank(agent.getName())) {
                return fallback;
            }
            // 未绑系统用户/用户已删：退化为纯坐席名
            if (agent.getUserId() == null || StrUtil.isBlank(agent.getUserName())) {
                return agent.getName();
            }
            return agent.getName() + "(" + agent.getUserName() + ")";
        } catch (Exception e) {
            // 快照查询失败不阻断话单更新，退化为兜底名
            return fallback;
        }
    }

}
