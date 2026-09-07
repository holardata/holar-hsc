// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.listener;


import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hsc.calltask.domain.CustomerCrowdEvent;
import com.hsc.calltask.domain.CustomerCrowdEventParam;
import com.hsc.calltask.domain.entity.CustomerCrowd;
import com.hsc.calltask.domain.entity.CustomerCrowdRel;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.query.CustomerCrowdQuery;
import com.hsc.calltask.domain.vo.CustomerCrowdVo;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.service.ICallTaskService;
import com.hsc.calltask.service.ICustomerCrowdRelService;
import com.hsc.calltask.service.ICustomerCrowdService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.domain.ConditionInfo;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.enums.RelationEnum;
import com.hsc.common.utils.StringUtils;
import com.hsc.common.utils.TraceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 客户群事件监听器
 *
 * @author danmo
 * @date 2025/7/2 22:19
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class CustomerCrowdEventListener implements ApplicationListener<CustomerCrowdEvent> {

    private final ICustomerCrowdService iCustomerCrowdService;
    private final ICustomerSeasService iCustomerSeasService;
    private final ICustomerCrowdRelService iCustomerCrowdRelService;
    private final ICallTaskService iCallTaskService;

    @Override
    public void onApplicationEvent(CustomerCrowdEvent event) {
        TraceUtil.setTraceId(UUID.randomUUID().toString().replaceAll("-", ""));
        try {
            log.info("客户群事件监听器 event:{}", JSONObject.toJSONString(event.getSource()));
            CustomerCrowdEventParam param = event.getCustomerCrowd();

            if(param.getEventType() == 1){
                //新增客户到人群
                addCustomerToCrowd(param.getCustomerId());
            }else if (param.getEventType() == 2){
                //从人群删除客户
                removeCustomerFromCrowd(param.getCustomerId());
            }else if (param.getEventType() == 3){
                //计算客户群数据
                calculationCrowdCustomer(param);
            }
        } finally {
            TraceUtil.clear();
        }
    }

    private void addCustomerToCrowd(Long customerId) {
        CustomerSeasVo customerSeas = iCustomerSeasService.getDetail(customerId);
        if(Objects.isNull(customerSeas)){
            log.info("客户不存在 customerId:{}", customerId);
            return;
        }
        // 人群互斥（设计 D7）：已归属坐席的客户不进任何自动人群——清掉旧关联（客户被分配进私海后
        // 编辑资料触发的重评估走这里，把分配前的残留关联清干净），与全量计算语义一致
        if (Objects.nonNull(customerSeas.getOwnerId())) {
            CustomerCrowdQuery cleanQuery = new CustomerCrowdQuery();
            cleanQuery.setStatus(1);
            cleanQuery.setType(2);
            List<CustomerCrowdVo> autoCrowds = iCustomerCrowdService.getList(cleanQuery);
            if (!CollectionUtils.isEmpty(autoCrowds)) {
                iCustomerCrowdRelService.remove(new LambdaQueryWrapper<CustomerCrowdRel>()
                        .eq(CustomerCrowdRel::getCustomerId, customerSeas.getId())
                        .in(CustomerCrowdRel::getCrowdId, autoCrowds.stream().map(CustomerCrowdVo::getId).toList()));
            }
            log.info("已归属客户不入人群(互斥) customerId:{} ownerId:{}", customerId, customerSeas.getOwnerId());
            return;
        }
        //查询人群列表
        CustomerCrowdQuery crowdQuery = new CustomerCrowdQuery();
        crowdQuery.setStatus(1);
        crowdQuery.setType(2);
        List<CustomerCrowdVo> customerCrowdList = iCustomerCrowdService.getList(crowdQuery);
        if(CollectionUtils.isEmpty(customerCrowdList)){
            log.info("没有客户群 customerId:{}", customerId);
            return;
        }

        JSONObject customerInfo = customerSeas.getCustomerInfo();
        List<Long> crowdIdList = new ArrayList<>();
        //判断是否符合人群筛选条件：条件间 AND（全部满足）、单条件多值间 OR（任一命中），与全量计算 buildQueryWrapper 语义对齐
        for (CustomerCrowdVo customerCrowd : customerCrowdList) {
            if (matchCustomerToCrowd(customerInfo, customerCrowd.getSwipe())) {
                crowdIdList.add(customerCrowd.getId());
            }
        }
        // 客户维度重评估：先清该客户在全部启用自动人群中的旧关联（含不再匹配的），再插入新关联。
        // 不能按人群维度删（batchUpsert 的语义是人群全量替换）——那会把人群里其他成员全部清空
        List<Long> autoCrowdIds = customerCrowdList.stream().map(CustomerCrowdVo::getId).toList();
        iCustomerCrowdRelService.remove(new LambdaQueryWrapper<CustomerCrowdRel>()
                .eq(CustomerCrowdRel::getCustomerId, customerSeas.getId())
                .in(CustomerCrowdRel::getCrowdId, autoCrowdIds));
        if (!CollectionUtils.isEmpty(crowdIdList)) {
            List<CustomerCrowdRel> rels = crowdIdList.stream()
                    .map(crowdId -> {
                        CustomerCrowdRel rel = new CustomerCrowdRel();
                        rel.setCustomerId(customerSeas.getId());
                        rel.setCrowdId(crowdId);
                        return rel;
                    })
                    .toList();
            iCustomerCrowdRelService.saveBatch(rels);
        }
    }

    /**
     * 客户与人群筛选条件整体匹配：条件间 AND，空条件不入人群
     */
    private boolean matchCustomerToCrowd(JSONObject customerInfo, List<ConditionInfo> swipe) {
        if (CollectionUtils.isEmpty(swipe)) {
            return false;
        }
        for (ConditionInfo conditionInfo : swipe) {
            if (!evaluateCondition(customerInfo, conditionInfo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单条件判定：单条件多值间 OR（任一命中即条件通过）
     */
    private boolean evaluateCondition(JSONObject customerInfo, ConditionInfo conditionInfo) {
        String fieldName = conditionInfo.getFieldName();
        RelationEnum anEnum = RelationEnum.getEnum(conditionInfo.getRelation());
        if (anEnum == null) {
            return false;
        }
        List<String> values = conditionInfo.getValue();
        // 为空/不为空：字段不存在（JSON_EXTRACT 为 NULL）同样视为"为空"，与全量计算 SQL 语义对齐
        switch (anEnum) {
            case NULL -> {
                return StringUtils.isNull(customerInfo.getString(fieldName));
            }
            case NOT_NULL -> {
                return StringUtils.isNotBlank(customerInfo.getString(fieldName));
            }
        }
        if (!customerInfo.containsKey(fieldName) || CollectionUtils.isEmpty(values)) {
            return false;
        }
        try {
            switch (anEnum) {
                case EQUAL, NOT_EQUAL, INCLUDE, NOT_INCLUDE -> {
                    String actual = customerInfo.getString(fieldName);
                    return values.stream().anyMatch(v -> matchText(anEnum, actual, v));
                }
                case MORE_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL -> {
                    java.math.BigDecimal actual = customerInfo.getBigDecimal(fieldName);
                    if (actual == null) {
                        return false;
                    }
                    return values.stream().anyMatch(v -> compareNumber(anEnum, actual, new java.math.BigDecimal(v)));
                }
                case INTERVAL -> {
                    if (values.size() != 2) {
                        return false;
                    }
                    java.math.BigDecimal actual = customerInfo.getBigDecimal(fieldName);
                    if (actual == null) {
                        return false;
                    }
                    java.math.BigDecimal low = new java.math.BigDecimal(values.get(0));
                    java.math.BigDecimal high = new java.math.BigDecimal(values.get(1));
                    return actual.compareTo(low) >= 0 && actual.compareTo(high) <= 0;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("人群条件判定异常 字段:{}，条件:{}", fieldName, JSONObject.toJSONString(conditionInfo), e);
            return false;
        }
    }

    private boolean matchText(RelationEnum anEnum, String actual, String value) {
        return switch (anEnum) {
            case EQUAL -> actual.equals(value);
            case NOT_EQUAL -> !actual.equals(value);
            case INCLUDE -> actual.contains(value);
            case NOT_INCLUDE -> !actual.contains(value);
            default -> false;
        };
    }

    private boolean compareNumber(RelationEnum anEnum, java.math.BigDecimal actual, java.math.BigDecimal value) {
        return switch (anEnum) {
            case MORE_THAN -> actual.compareTo(value) > 0;
            case GREATER_EQUAL -> actual.compareTo(value) >= 0;
            case LESS_THAN -> actual.compareTo(value) < 0;
            case LESS_EQUAL -> actual.compareTo(value) <= 0;
            default -> false;
        };
    }

    private void removeCustomerFromCrowd(Long customerId) {
        CustomerCrowdRel customerCrowdRel = new CustomerCrowdRel();
        customerCrowdRel.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
        iCustomerCrowdRelService.update(customerCrowdRel,new LambdaQueryWrapper<CustomerCrowdRel>().eq(CustomerCrowdRel::getCustomerId,customerId).eq(BaseEntity::getDelFlag,DeleteStatusEnum.DELETE_NO.getIndex()));
    }

    private void calculationCrowdCustomer(CustomerCrowdEventParam param) {
        try {
            CustomerCrowdVo customerCrowd = iCustomerCrowdService.getDetail(param.getCrowdId());
            // 更新进度
            iCustomerCrowdService.update(new LambdaUpdateWrapper<CustomerCrowd>()
                    .eq(CustomerCrowd::getId, customerCrowd.getId())
                    .set(CustomerCrowd::getProgress, 2));

            //查看筛选条件
            List<ConditionInfo> swipe = customerCrowd.getSwipe();
            if (CollectionUtils.isEmpty(swipe)) {
                // 空条件人群无意义，标记失败并给出原因，避免 progress 卡在"计算中"
                iCustomerCrowdService.update(new LambdaUpdateWrapper<CustomerCrowd>()
                        .eq(CustomerCrowd::getId, customerCrowd.getId())
                        .set(CustomerCrowd::getProgress, 4)
                        .set(CustomerCrowd::getReason, "筛选条件为空"));
                return;
            }
            Long lastId = 0L;
            int pageSize = 1000;
            int count = 0;
            //根据筛选条件查询客户公海
            while (true) {
                LambdaQueryWrapper<CustomerSeas> wrapper = buildQueryWrapper(swipe, lastId, pageSize);
                try {
                    List<CustomerSeas> customerSeasList = iCustomerSeasService.list(wrapper);
                    if (CollectionUtils.isEmpty(customerSeasList)) {
                        break;
                    }
                    List<Long> customerSeasIds = customerSeasList.stream().map(CustomerSeas::getId).toList();
                    // 先推进游标再处理，处理异常才不会卡死循环重读同一页
                    lastId = customerSeasIds.get(customerSeasIds.size() - 1);
                    count += customerSeasIds.size();
                    // 批量处理保存
                    processAndSave(customerCrowd.getId(), customerSeasIds);
                } catch (Exception e) {
                    log.error("客户群事件监听器异常 crowdId:{}, lastId:{}", param.getCrowdId(), lastId, e);
                    // 查询失败时游标无法推进，吞掉异常会无限重读同一页（死循环刷错误日志、progress 永卡"计算中"）；
                    // 上抛由外层统一置 progress=4 计算失败，让失败原因对用户可见
                    throw e;
                }
            }

            iCustomerCrowdService.update(new LambdaUpdateWrapper<CustomerCrowd>()
                    .eq(CustomerCrowd::getId, customerCrowd.getId())
                    .set(CustomerCrowd::getProgress, 3)
                    .set(CustomerCrowd::getCrowdNum, count));
        } catch (Exception e) {
            log.error("客户群事件监听器异常 event:{}", JSONObject.toJSONString(param), e);
            CustomerCrowdVo customerCrowd = iCustomerCrowdService.getDetail(param.getCrowdId());
            // reason 列 varchar(200)：整段异常 message 直接写入会超长，导致"置失败"本身失败、人群卡死在"计算中"
            iCustomerCrowdService.update(new LambdaUpdateWrapper<CustomerCrowd>()
                    .eq(CustomerCrowd::getId, customerCrowd.getId())
                    .set(CustomerCrowd::getProgress, 4)
                    .set(CustomerCrowd::getReason, StringUtils.substring(e.getMessage(), 0, 200)));
        }
    }


    private LambdaQueryWrapper<CustomerSeas> buildQueryWrapper(List<ConditionInfo> swipe, Long lastId, int pageSize) {
        LambdaQueryWrapper<CustomerSeas> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CustomerSeas::getId);
        wrapper.gt(CustomerSeas::getId, lastId);
        wrapper.last("limit " + pageSize);
        wrapper.orderByAsc(CustomerSeas::getId);
        wrapper.eq(CustomerSeas::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex());
        // 人群互斥（设计 D7 改判）：已归属坐席（私海）客户不进入人群——任务名单供给只含无主公海客户；
        // 收回公海后下次重算重新参与判定。与增量评估（addCustomerToCrowd 开头跳过）语义一致
        wrapper.isNull(CustomerSeas::getOwnerId);
        swipe.forEach(conditionInfo -> {
            RelationEnum anEnum = RelationEnum.getEnum(conditionInfo.getRelation());
            switch (anEnum) {
                case EQUAL, NOT_EQUAL, MORE_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, INCLUDE,
                     NOT_INCLUDE -> {
                    String jsonSql = anEnum.getJsonFormat();
                    if (conditionInfo.getValue().size() == 1) {
                        wrapper.apply(jsonSql, conditionInfo.getFieldName(), conditionInfo.getValue().get(0));
                    } else if (conditionInfo.getValue().size() > 1) {
                        wrapper.and(wrap ->
                                conditionInfo.getValue().forEach(val ->
                                        wrap.or(w -> w.apply(jsonSql, conditionInfo.getFieldName(), val))
                                ));
                    }
                }
                case INTERVAL -> {
                    String jsonSql = anEnum.getJsonFormat();
                    wrapper.apply(jsonSql, conditionInfo.getFieldName(), conditionInfo.getValue().get(0), conditionInfo.getValue().get(1));
                }
                case NULL, NOT_NULL -> {
                    String jsonSql = anEnum.getJsonFormat();
                    wrapper.apply(jsonSql, conditionInfo.getFieldName());
                }
            }
        });
        return wrapper;
    }

    private void processAndSave(Long crowdId, List<Long> customerSeasIds) {
        if (CollectionUtils.isEmpty(customerSeasIds)) {
            return;
        }
        List<CustomerCrowdRel> rels = customerSeasIds.stream()
                .map(customerId -> {
                    CustomerCrowdRel rel = new CustomerCrowdRel();
                    rel.setCustomerId(customerId);
                    rel.setCrowdId(crowdId);
                    return rel;
                })
                .toList();

        iCustomerCrowdRelService.batchUpsert(Collections.singletonList(crowdId),rels);
    }





    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }
}
