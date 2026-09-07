// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SipAgent;
import com.hsc.system.domain.query.agent.SipAgentAddQuery;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.vo.agent.SipAgentConfigVo;
import com.hsc.system.domain.vo.agent.SipAgentStatusVo;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;

import java.util.List;

/**
 * 坐席管理表(SipAgent)
 *
 * @author danmo
 * @date 2023-09-26 11:08:58
 */
public interface ISipAgentService extends IBaseService<SipAgent> {

    void add(SipAgentAddQuery query);

    void update(SipAgentAddQuery query);

    void delete(SipAgentQuery query);

    SipAgentVo getDetail(Long id);

    PageInfo<SipAgentVo> getPageList(SipAgentQuery query);

    List<SipAgentVo> getInfoByQuery(SipAgentQuery query);

    /**
     * 登录用户绑定的坐席ID（统一入口：任务拨打/私海等 userId→agentId 解析复用，未绑定返回 null）
     */
    Long getAgentIdByUserId(Long userId);

    SipAgentVo getInfoByAgent(String agentNum);

    Boolean updateStatus(Long id, Integer status);

    List<SipAgentStatusVo> getAgentStatusList(List<Long> agentIds);

    void updateOnlineStatus(Long id, Integer onlineStatus, Long timestamp);

    SipAgentConfigVo getAgentSipConfig(Long userId);

    /**
     * 可绑定的 SIP 号码（排除已被其他坐席绑定的）。
     * @param excludeAgentId 编辑场景传当前坐席 id，其已绑的号会被保留（否则下拉为空）
     */
    List<SipSimpleVo> availableSipNumbers(Long excludeAgentId);

    /**
     * 话单坐席名快照："坐席名(登录名)"——固化通话当时坐席绑定的系统用户，防后续改绑后
     * 历史话单对不上人（话单/转写记录是日志性数据，落库后不再变）。
     * @param agentId  坐席 id；null（AI 坐席/未分配等）或查不到坐席/未绑用户时返回 fallback
     * @param fallback 兜底名（一般传 CallInfo.getAgentName()，如"AI智能坐席"）
     */
    String buildNameSnapshot(Long agentId, String fallback);
}
