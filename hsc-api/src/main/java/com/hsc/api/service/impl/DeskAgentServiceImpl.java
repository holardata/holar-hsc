package com.hsc.api.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.hsc.api.service.IDeskAgentService;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.enums.SipAgentStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.esl.service.ISipRegService;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.service.ISipAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 座机坐席会话实现。注册判定用 sofia reg 实时查询（Redis onlineStatus 靠前端 WS 推，
 * 异常断开会假在线；sofia reg 反映分机真实注册状态，与 FsAgentRouteHandler 同一判据）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DeskAgentServiceImpl implements IDeskAgentService {

    private final ISipAgentService iSipAgentService;
    private final ISipRegService sipRegService;
    private final RedisService redisService;

    @Override
    public void signIn(Long userId) {
        SipAgentVo agent = getBoundAgent(userId);
        if (StringUtils.isBlank(agent.getAgentNumber())) {
            throw new CommonException("坐席未配置分机号");
        }
        //话机未注册(断电/掉线/未配置)拒绝签入：签了也分配不到可用的通话通道
        if (CollectionUtil.isEmpty(sipRegService.getList(agent.getAgentNumber()))) {
            throw new CommonException("话机未注册，请检查话机网络/账号配置");
        }
        //与软电话签入同一写入入口（updateOnlineStatus 内含状态表兜底创建），分配器无差别可见
        iSipAgentService.updateOnlineStatus(agent.getId(), SipAgentStatusEnum.READY.getCode(), System.currentTimeMillis());
        log.info("座机签入成功 agentId:{} agentNumber:{} userId:{}", agent.getId(), agent.getAgentNumber(), userId);
    }

    @Override
    public void signOut(Long userId) {
        SipAgentVo agent = getBoundAgentOrNull(userId);
        if (Objects.isNull(agent)) {
            return;
        }
        //先置离线（同步 DB online_status 与残留引用的可见状态）再从状态表移除：
        //移除后轮流/空闲两种分配策略都查不到该坐席，不再分配新客户
        iSipAgentService.updateOnlineStatus(agent.getId(), SipAgentStatusEnum.OFF_ON.getCode(), System.currentTimeMillis());
        redisService.delCacheMapValue(CacheConstants.AGENT_CURRENT_STATUS_KEY, String.valueOf(agent.getId()));
        log.info("座机签出 agentId:{} agentNumber:{} userId:{}", agent.getId(), agent.getAgentNumber(), userId);
    }

    @Override
    public boolean isPhoneRegistered(Long userId) {
        SipAgentVo agent = getBoundAgentOrNull(userId);
        if (Objects.isNull(agent) || StringUtils.isBlank(agent.getAgentNumber())) {
            return false;
        }
        return CollectionUtil.isNotEmpty(sipRegService.getList(agent.getAgentNumber()));
    }

    private SipAgentVo getBoundAgent(Long userId) {
        SipAgentVo agent = getBoundAgentOrNull(userId);
        if (Objects.isNull(agent)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        return agent;
    }

    private SipAgentVo getBoundAgentOrNull(Long userId) {
        if (Objects.isNull(userId)) {
            return null;
        }
        SipAgentQuery query = new SipAgentQuery();
        query.setUserId(userId);
        List<SipAgentVo> agents = iSipAgentService.getInfoByQuery(query);
        return CollectionUtil.isEmpty(agents) ? null : agents.get(0);
    }
}
