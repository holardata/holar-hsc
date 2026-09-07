package com.hsc.calltask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.calltask.domain.entity.CustomerPoolLog;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.query.CustomerPoolAssignQuery;
import com.hsc.calltask.domain.query.CustomerPoolReleaseQuery;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.domain.vo.CustomerTransferTimelineVo;
import com.hsc.calltask.mapper.CustomerPoolLogMapper;
import com.hsc.calltask.service.ICustomerPoolLogService;
import com.hsc.calltask.service.ICustomerPoolService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.service.ISipAgentService;
import cn.hutool.core.collection.CollectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 客户归属（公海私海）服务实现：管理员分配制（设计 D6）。
 *
 * <p>私海 = customer_seas 同表两视图（owner_id 空=公海）。分配走条件 UPDATE
 * （WHERE owner_id IS NULL）乐观锁防并发双分——两个管理员同时分配同一客户仅一次成功；
 * 收回清 owner，当晚人群重算后重新可圈选（人群互斥见 CustomerCrowdEventListener）。
 * 每个归属动作记 customer_pool_log（只增不改），与拨打历史在客户详情时间线混排。
 *
 * @author danmo
 * @since 2026-09-03
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class CustomerPoolServiceImpl extends BaseServiceImpl<CustomerPoolLogMapper, CustomerPoolLog> implements ICustomerPoolService {

    /** 流转日志动作：1-导入入库 2-分配 3-收回 */
    private static final int ACTION_IMPORT = 1;
    private static final int ACTION_ASSIGN = 2;
    private static final int ACTION_RELEASE = 3;

    private final ICustomerSeasService customerSeasService;
    private final ICustomerPoolLogService customerPoolLogService;
    private final ISipAgentService sipAgentService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer assign(CustomerPoolAssignQuery query, Long operatorUserId) {
        SipAgentQuery agentQuery = new SipAgentQuery();
        agentQuery.setId(query.getAgentId());
        List<SipAgentVo> agents = sipAgentService.getInfoByQuery(agentQuery);
        if (CollectionUtil.isEmpty(agents)) {
            throw new CommonException("无效坐席ID");
        }
        Date now = new Date();
        int success = 0;
        for (Long customerId : query.getCustomerIds()) {
            // 条件 UPDATE 乐观锁：仅无主客户可被分配，并发双分仅一次生效
            boolean updated = customerSeasService.update(new LambdaUpdateWrapper<CustomerSeas>()
                    .eq(CustomerSeas::getId, customerId)
                    .isNull(CustomerSeas::getOwnerId)
                    .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                    .set(CustomerSeas::getOwnerId, query.getAgentId())
                    .set(CustomerSeas::getOwnerTime, now));
            if (!updated) {
                log.info("客户分配跳过(已被分配或不存在) customerId:{} agentId:{}", customerId, query.getAgentId());
                continue;
            }
            savePoolLog(customerId, ACTION_ASSIGN, null, query.getAgentId(), operatorUserId, null, now);
            success++;
        }
        return success;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer release(CustomerPoolReleaseQuery query, Long operatorUserId) {
        List<CustomerSeas> owned = customerSeasService.list(new LambdaUpdateWrapper<CustomerSeas>()
                .in(CustomerSeas::getId, query.getCustomerIds())
                .isNotNull(CustomerSeas::getOwnerId)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (CollectionUtil.isEmpty(owned)) {
            return 0;
        }
        Date now = new Date();
        // 收回：清归属（仅对当前有主的行生效）
        customerSeasService.update(new LambdaUpdateWrapper<CustomerSeas>()
                .in(CustomerSeas::getId, owned.stream().map(CustomerSeas::getId).toList())
                .set(CustomerSeas::getOwnerId, null)
                .set(CustomerSeas::getOwnerTime, null));
        for (CustomerSeas customer : owned) {
            savePoolLog(customer.getId(), ACTION_RELEASE, customer.getOwnerId(), null, operatorUserId, query.getReason(), now);
        }
        return owned.size();
    }

    @Override
    public List<CustomerSeasVo> myPageList(CustomerSeasQuery query, Long userId) {
        Long agentId = getAgentIdByUserId(userId);
        if (Objects.isNull(agentId)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        // 服务端覆写过滤条件：仅本人私海，防越权（前端传 ownerId 无效）
        query.setOwnerId(agentId);
        query.setOnlyPublic(null);
        return customerSeasService.pageList(query);
    }

    @Override
    public List<CustomerTransferTimelineVo> getTransferTimeline(Long customerId, Integer pageIndex, Integer pageSize) {
        if (Objects.isNull(customerId)) {
            throw new CommonException("客户ID不能为空");
        }
        super.startPage(pageIndex, pageSize);
        return this.baseMapper.getTransferTimeline(customerId);
    }

    /**
     * 登录用户绑定的坐席ID（统一入口 ISipAgentService.getAgentIdByUserId，未绑定返回 null）
     */
    private Long getAgentIdByUserId(Long userId) {
        return sipAgentService.getAgentIdByUserId(userId);
    }

    private void savePoolLog(Long customerId, Integer action, Long fromAgent, Long toAgent, Long operator, String reason, Date time) {
        CustomerPoolLog poolLog = new CustomerPoolLog();
        poolLog.setCustomerId(customerId);
        poolLog.setAction(action);
        poolLog.setFromAgent(fromAgent);
        poolLog.setToAgent(toAgent);
        poolLog.setOperator(operator);
        poolLog.setReason(reason);
        poolLog.setCreateTime(time);
        customerPoolLogService.save(poolLog);
    }
}
