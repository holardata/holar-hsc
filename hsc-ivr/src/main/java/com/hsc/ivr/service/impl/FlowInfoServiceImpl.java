// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.ivr.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.ivr.domain.entity.FlowInfo;
import com.hsc.ivr.domain.query.FlowInfoAddQuery;
import com.hsc.ivr.domain.query.FlowInfoQuery;
import com.hsc.ivr.domain.vo.FlowEdgeVo;
import com.hsc.ivr.domain.vo.FlowInfoListVo;
import com.hsc.ivr.domain.vo.FlowInfoVo;
import com.hsc.ivr.domain.vo.FlowNodeVo;
import com.hsc.ivr.mapper.FlowInfoMapper;
import com.hsc.ivr.service.IFlowInfoService;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ivr流程信息(FlowInfo)表服务实现类
 *
 * @author danmo
 * @since 2024-12-23 15:08:24
 */
@RequiredArgsConstructor
@Service
public class FlowInfoServiceImpl extends BaseServiceImpl<FlowInfoMapper, FlowInfo> implements IFlowInfoService {

    private final ISysUserService sysUserService;
    private final ApplicationEventPublisher publisher;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(FlowInfoAddQuery query) {
        FlowInfo flowInfo = new FlowInfo();
        flowInfo.setName(query.getName());
        flowInfo.setDesc(query.getDesc());
        flowInfo.setStatus(query.getStatus());
        flowInfo.setGroupId(query.getGroupId());
        flowInfo.setFlowData(query.getFlowData());
        save(flowInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(FlowInfoAddQuery query) {
        FlowInfo flowInfo = getById(query.getId());
        if (Objects.isNull(flowInfo)) {
            throw new CommonException("无效ID");
        }
        if (!StringUtils.equals(flowInfo.getName(), query.getName()) && checkName(query.getName())) {
            throw new CommonException("名称已存在");
        } else {
            flowInfo.setName(query.getName());
        }
        if (StringUtils.isNotBlank(query.getDesc())) {
            flowInfo.setDesc(query.getDesc());
        }
        if (Objects.nonNull(query.getStatus())) {
            flowInfo.setStatus(query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getFlowData())) {
            flowInfo.setFlowData(query.getFlowData());
        }
        updateById(flowInfo);
        // 保存只写草稿（flow_data），不动 published_flow_data/version：编辑不影响线上
        // 与进行中通话（运行时读发布快照+版本化 Redis 缓存，见 FlowEventListener）
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        FlowInfo flowInfo = getById(id);
        if (Objects.isNull(flowInfo)) {
            throw new CommonException("无效ID");
        }
        flowInfo.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        updateById(flowInfo);
        // 不清运行缓存：流程缓存 key 带发布版本号（见 CacheConstants），
        // 进行中通话钉死旧版本可继续走完，旧缓存靠 TTL 过期
    }

    @Override
    public FlowInfoVo getInfo(Long id) {
        return baseMapper.getInfo(id);
    }

    @Override
    public List<FlowInfoListVo> pageList(FlowInfoQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<FlowInfoListVo> flowInfoList = this.baseMapper.getList(query);
        if (CollectionUtils.isNotEmpty(flowInfoList)) {
            sysUserService.decorate(flowInfoList);
        }
        return flowInfoList;
    }

    @Override
    public void publish(Long id) {
        FlowInfo flowInfo = getById(id);
        if (Objects.isNull(flowInfo)) {
            throw new CommonException("无效ID");
        }
        // 发布兜底结构校验：防前端绕过或库中脏数据直接上线（业务配置完整性由前端发布校验负责）
        validateFlowStructure(flowInfo.getFlowData());
        // 草稿快照进 published_flow_data + 版本+1：新通话用新版本（新版本 Redis 缓存 key 不同，
        // 首次构建时重建），进行中通话钉死旧版本不受影响
        flowInfo.setPublishedFlowData(flowInfo.getFlowData());
        flowInfo.setVersion((flowInfo.getVersion() == null ? 0 : flowInfo.getVersion()) + 1);
        flowInfo.setStatus(2);
        updateById(flowInfo);

    }

    /**
     * 发布前结构校验（与 FlowEventListener.buildStateMachine 的转移注册规则对齐）：
     * ①流程数据可解析且含 start/end 节点；②每条边的 source/target 都在节点集内；
     * ③event 非空且同 source 下不重复（重复转移会让 Spring StateMachine 构建抛异常，
     * 通话进来直接无下文）；④event 不能用保留字 "end"（会与每节点的结束兜底转移撞车）
     */
    private void validateFlowStructure(String flowData) {
        if (StringUtils.isBlank(flowData)) {
            throw new CommonException("流程数据为空，无法发布");
        }
        JSONObject flowJson;
        try {
            flowJson = JSONObject.parseObject(flowData);
        } catch (Exception e) {
            throw new CommonException("流程数据解析失败，无法发布");
        }
        List<FlowNodeVo> nodes = flowJson.getObject("nodes", new TypeReference<List<FlowNodeVo>>() {
        });
        List<FlowEdgeVo> edges = flowJson.getObject("edges", new TypeReference<List<FlowEdgeVo>>() {
        });
        if (CollectionUtils.isEmpty(nodes)) {
            throw new CommonException("流程没有任何节点，无法发布");
        }
        Set<String> nodeIds = nodes.stream().map(FlowNodeVo::getId).collect(Collectors.toSet());
        if (!nodeIds.contains("start")) {
            throw new CommonException("流程缺少开始节点，无法发布");
        }
        if (!nodeIds.contains("end")) {
            throw new CommonException("流程缺少结束节点，无法发布");
        }
        if (CollectionUtils.isEmpty(edges)) {
            throw new CommonException("流程没有任何连线，无法发布");
        }
        Set<String> transitions = new HashSet<>();
        for (FlowEdgeVo edge : edges) {
            String event = edge.getEvent();
            if (StringUtils.isBlank(event)) {
                throw new CommonException("存在未推导出流转事件的连线，无法发布");
            }
            if ("end".equals(event)) {
                throw new CommonException("连线流转事件不能使用保留字 end，无法发布");
            }
            if (!nodeIds.contains(edge.getSourceNodeId()) || !nodeIds.contains(edge.getTargetNodeId())) {
                throw new CommonException("连线「" + edge.getSourceNodeId() + " → " + edge.getTargetNodeId() + "」指向不存在的节点，无法发布");
            }
            if (!transitions.add(edge.getSourceNodeId() + ":" + event)) {
                throw new CommonException("节点「" + edge.getSourceNodeId() + "」存在重复的流转事件「" + event + "」，运行时状态机构建会失败，无法发布");
            }
        }
    }

    @Override
    public void offline(Long id) {
        FlowInfo flowInfo = getById(id);
        if (Objects.isNull(flowInfo)) {
            throw new CommonException("无效ID");
        }
        flowInfo.setStatus(1);
        updateById(flowInfo);
    }


    private boolean checkName(String name) {
        return null != getOne(new LambdaQueryWrapper<FlowInfo>().eq(FlowInfo::getName, name).last("limit 1"));
    }
}

