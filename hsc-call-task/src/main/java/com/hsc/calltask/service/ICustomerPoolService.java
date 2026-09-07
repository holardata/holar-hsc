package com.hsc.calltask.service;

import com.hsc.calltask.domain.query.CustomerPoolAssignQuery;
import com.hsc.calltask.domain.query.CustomerPoolReleaseQuery;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.domain.vo.CustomerTransferTimelineVo;

import java.util.List;

/**
 * 客户归属（公海私海）服务：管理员分配制——分配（乐观锁防并发双分）、收回、
 * 我的客户视图、客户流转时间线（设计 D6/D10）
 *
 * @author danmo
 * @since 2026-09-03
 */
public interface ICustomerPoolService {

    /**
     * 分配客户给坐席（进私海）：仅无主客户可分配（条件 UPDATE 乐观锁），每个成功分配记流转日志
     *
     * @return 成功分配数（已被分配/已删除的跳过）
     */
    Integer assign(CustomerPoolAssignQuery query, Long operatorUserId);

    /**
     * 收回坐席私海客户回公海（含原因），每个成功收回记流转日志
     *
     * @return 成功收回数
     */
    Integer release(CustomerPoolReleaseQuery query, Long operatorUserId);

    /**
     * 我的客户分页（owner=登录坐席，服务端身份过滤防越权）
     */
    List<CustomerSeasVo> myPageList(CustomerSeasQuery query, Long userId);

    /**
     * 客户流转时间线：归属动作 + 外呼明细按时间混排（金蝶订单日志式）
     */
    List<CustomerTransferTimelineVo> getTransferTimeline(Long customerId, Integer pageIndex, Integer pageSize);
}
