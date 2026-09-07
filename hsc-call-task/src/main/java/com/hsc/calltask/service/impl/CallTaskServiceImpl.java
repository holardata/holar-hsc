// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.calltask.domain.CallTaskContactImportEvent;
import com.hsc.calltask.domain.entity.CallTaskAssignment;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.entity.CallTaskDialLog;
import com.hsc.calltask.domain.query.CallTaskAddQuery;
import com.hsc.calltask.domain.query.CallTaskContactImportQuery;
import com.hsc.calltask.domain.query.CallTaskContactQuery;
import com.hsc.calltask.domain.query.CallTaskQuery;
import com.hsc.calltask.domain.query.CallTaskDialLogQuery;
import com.hsc.calltask.domain.query.CallTaskDispositionQuery;
import com.hsc.calltask.domain.vo.CallTaskDialLogVo;
import com.hsc.calltask.domain.vo.CallTaskContactImportResultVo;
import com.hsc.calltask.domain.vo.CallTaskContactVo;
import com.hsc.calltask.domain.vo.CallTaskVo;
import com.hsc.calltask.domain.vo.CallTaskMySummaryVo;
import com.hsc.calltask.service.ICallTaskAssignmentService;
import com.hsc.calltask.service.ICustomerCrowdService;
import com.hsc.calltask.service.ICallTaskDialLogService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.calltask.service.IPredictiveDialerService;
import com.hsc.common.base.BaseEntity;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.calltask.mapper.CallTaskMapper;
import com.hsc.calltask.domain.vo.TaskProgressVo;
import com.hsc.calltask.domain.entity.CallTask;
import com.hsc.calltask.service.ICallTaskService;
import com.hsc.calltask.util.FieldHeaders;
import com.hsc.common.enums.CallTaskStatusEnum;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.CallDisplayPool;
import com.hsc.system.domain.query.agent.SipAgentQuery;
import com.hsc.system.domain.vo.agent.SipAgentVo;
import com.hsc.system.domain.vo.agent.SipSimpleAgent;
import com.hsc.system.service.ICallDisplayPoolService;
import com.hsc.system.service.ISipAgentService;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 外呼任务表(CallTask)表服务实现类
 *
 * @author danmo
 * @since 2025-07-08 10:42:38
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CallTaskServiceImpl extends BaseServiceImpl<CallTaskMapper, CallTask> implements ICallTaskService {

    private final IPredictiveDialerService predictiveDialerService;
    private final ISysUserService sysUserService;
    private final ICallDisplayPoolService callDisplayPoolService;
    private final ICustomerCrowdService customerCrowdService;
    private final ICustomerSeasService customerSeasService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ICallTaskAssignmentService callTaskAssignmentService;
    private final ICallTaskDialLogService callTaskDialLogService;
    private final ISipAgentService sipAgentService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(CallTaskAddQuery query) {
        Boolean isExist =checkName(query.getName());
        if(isExist){
            throw new CommonException("任务名称已存在");
        }
        CallTask task = new CallTask();
        task.setName(query.getName());
        task.setPriority(query.getPriority());
        task.setStartDay(query.getStartDay());
        task.setEndDay(query.getEndDay());
        task.setSTime(query.getSTime());
        task.setETime(query.getETime());
        task.setWorkCycle(query.getWorkCycle());
        task.setStatus(CallTaskStatusEnum.NOT_START.getCode());
        task.setAssignmentType(query.getAssignmentType());
        task.setAgentList(JSONObject.toJSONString(query.getAgentList()));
        task.setPhonePoolId(query.getPhonePoolId());
        task.setRemark(query.getRemark());
        if(save(task)){
            predictiveDialerService.createTask(task.getId(),task.getPriority(),query.getStartDay(),query.getEndDay(),query.getSTime(),query.getETime(),query.getWorkCycle());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(CallTaskAddQuery query) {
        CallTask callTask = getById(query.getId());
        if(Objects.isNull(callTask)){
            throw new CommonException("无效ID");
        }
        if(!Objects.equals(callTask.getName(), query.getName())){
            Boolean isExist =checkName(query.getName());
            if(isExist){
                throw new CommonException("任务名称已存在");
            }
        }
        CallTask task = new CallTask();
        task.setId(query.getId());
        task.setName(query.getName());
        task.setPriority(query.getPriority());
        task.setStartDay(query.getStartDay());
        task.setEndDay(query.getEndDay());
        task.setSTime(query.getSTime());
        task.setETime(query.getETime());
        task.setWorkCycle(query.getWorkCycle());
        task.setStatus(CallTaskStatusEnum.NOT_START.getCode());
        task.setAssignmentType(query.getAssignmentType());
        task.setAgentList(JSONObject.toJSONString(query.getAgentList()));
        task.setPhonePoolId(query.getPhonePoolId());
        task.setRemark(query.getRemark());
        if(updateById(task)){
            predictiveDialerService.createTask(task.getId(), task.getPriority(), query.getStartDay(),query.getEndDay(),query.getSTime(),query.getETime(),query.getWorkCycle());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void detele(CallTaskQuery query) {
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
        List<CallTask> list = ids.stream().map(id -> {
            CallTask callTask = new CallTask();
            callTask.setId(id);
            callTask.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return callTask;
        }).toList();
        if (updateBatchById(list)) {
            predictiveDialerService.deleteTask(ids);
        }
    }

    @Override
    public CallTaskVo getDetail(Long id) {
        CallTask task = getById(id);
        if (Objects.isNull(task)) {
            throw new CommonException("无效ID");
        }
        CallTaskVo taskVo = new CallTaskVo();
        BeanUtils.copyProperties(task, taskVo, "agentList");
        taskVo.setAgentList(JSONArray.parseArray(task.getAgentList(), SipSimpleAgent.class));
        if (Objects.nonNull(task.getPhonePoolId())) {
            CallDisplayPool displayPool = callDisplayPoolService.getById(task.getPhonePoolId());
            if (Objects.nonNull(displayPool)) {
                taskVo.setPhonePoolName(displayPool.getName());
            }
        }
        return taskVo;
    }

    @Override
    public List<CallTaskVo> pageList(CallTaskQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<CallTaskVo> list = getList(query);
        if(CollectionUtil.isNotEmpty(list)){
            sysUserService.decorate(list);
        }
        return list;
    }

    @Override
    public List<CallTaskVo> getList(CallTaskQuery query) {
        return this.baseMapper.getList(query);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void pauseTask(Long id) {
        CallTask task = getById(id);
        if (Objects.isNull(task)) {
            throw new CommonException("无效ID");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.PAUSE.getCode())) {
            throw new CommonException("任务已暂停");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.NOT_START.getCode())) {
            throw new CommonException("任务未开始");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.END.getCode())) {
            throw new CommonException("任务已结束");
        }
        Boolean updated = updateStatus(id, CallTaskStatusEnum.PAUSE);
        if (updated) {
            predictiveDialerService.pauseTask(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void startTask(Long id) {
        CallTask task = getById(id);
        if (Objects.isNull(task)) {
            throw new CommonException("无效ID");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.PROCESSING.getCode())) {
            throw new CommonException("任务已开始");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.END.getCode())) {
            throw new CommonException("任务已结束");
        }
        Boolean updated = updateStatus(id, CallTaskStatusEnum.PROCESSING);
        if (updated) {
            predictiveDialerService.resumeTask(id);
        }
    }

    @Override
    public void endTask(Long id) {
        CallTask task = getById(id);
        if (Objects.isNull(task)) {
            throw new CommonException("无效ID");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.END.getCode())) {
            throw new CommonException("任务已结束");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.NOT_START.getCode())) {
            throw new CommonException("任务未开始");
        }
        Boolean updated = updateStatus(id, CallTaskStatusEnum.END);
        if (updated) {
            predictiveDialerService.deleteTask(id);
        }
    }

    @Override
    public Boolean updateStatus(Long id, CallTaskStatusEnum callTaskStatusEnum) {
        CallTask task = new CallTask();
        task.setId(id);
        task.setStatus(callTaskStatusEnum.getCode());
        return updateById(task);
    }

    @Override
    public List<CallTaskContactVo> getTaskContactPageList(CallTaskContactQuery query) {
        // 任务视角（/task/customer/list）：按任务查联系人，缺 taskId 会退化为全库扫描，须拦截；
        // 坐席视角 my/list 跨任务查不走此校验（见 getMyTaskContactPageList）
        if (Objects.isNull(query.getTaskId())) {
            throw new CommonException("任务ID不能为空");
        }
        super.startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return getTaskContactList(query);
    }

    @Override
    public List<CallTaskContactVo> getTaskContactList(CallTaskContactQuery query)
    {
        return this.baseMapper.getTaskContactList(query);
    }


    @Override
    public CallTaskContactImportResultVo importTaskContact(CallTaskContactImportQuery query, MultipartFile file) {
        if (Objects.isNull(query.getTaskId())) {
            throw new CommonException("任务ID不能为空");
        }
        if(Objects.isNull(query.getImportType())){
            throw new CommonException("导入方式不能为空");
        }
        CallTask task = getById(query.getTaskId());
        if (Objects.isNull(task)) {
            throw new CommonException("无效任务ID");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.PROCESSING.getCode())) {
            throw new CommonException("任务已开始");
        }
        if (Objects.equals(task.getStatus(), CallTaskStatusEnum.END.getCode())) {
            throw new CommonException("任务已结束");
        }
        CallTaskContactImportResultVo result = new CallTaskContactImportResultVo();
        switch (query.getImportType()) {
            case 0 -> {
                if (Objects.isNull(query.getCrowdId())) {
                    throw new CommonException("人群ID不能为空");
                }
                List<Long> customerIds = customerCrowdService.getCustomerIdByCrowdId(query.getCrowdId());
                if (CollectionUtil.isEmpty(customerIds)) {
                    return result;
                }
                List<CustomerSeas> customers = customerSeasService.listByIds(customerIds);
                // 预检：任务内重复跳过 + 跨进行中任务/私海归属冲突（未 force 时返回不导入）
                Set<String> pending = precheckImport(query.getTaskId(),
                        customers.stream().map(CustomerSeas::getPhone).toList(),
                        Boolean.TRUE.equals(query.getForce()), result);
                if (Boolean.TRUE.equals(result.getConflict()) && !Boolean.TRUE.equals(query.getForce())) {
                    return result;
                }
                for (CustomerSeas customer : customers) {
                    if (Objects.isNull(customer.getPhone()) || !pending.contains(customer.getPhone().trim())) {
                        continue;
                    }
                    applicationEventPublisher.publishEvent(new CallTaskContactImportEvent(customer.getId(),
                            query.getTaskId(), query.getImportType(), query.getCrowdId(), null, null));
                    result.setImported(result.getImported() + 1);
                }
            }
            case 1 -> {
                if (Objects.isNull(file)) {
                    throw new CommonException("文件不能为空");
                }
                if(Objects.isNull(query.getTemplateId())){
                    throw new CommonException("模板ID不能为空");
                }
                // 先全量读行（预检需完整号码集合），预检通过后再逐行 publish 异步落库
                List<JSONObject> rows = new ArrayList<>();
                try {
                    EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                        private Map<Integer, String> headMap;

                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            this.headMap = headMap;
                        }

                        @Override
                        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                            if (headMap == null) return; // 确保表头已读取
                            JSONObject rowMap = new JSONObject();
                            for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                                Integer colIndex = entry.getKey();
                                String headerName = headMap.get(colIndex);
                                String cellValue = entry.getValue();
                                // 表头"显示名(fieldName)"解析字段标识作 key(与公海导入共用)，供监听器按 fieldName 取锚点值
                                rowMap.put(FieldHeaders.fieldNameOf(headerName), cellValue);
                            }
                            rows.add(rowMap);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            log.info("所有数据解析完成！");
                        }
                    }).sheet().doRead();
                } catch (Exception e) {
                    throw new CommonException("导入失败");
                }
                Set<String> pending = precheckImport(query.getTaskId(),
                        rows.stream().map(r -> r.getString("phone")).toList(),
                        Boolean.TRUE.equals(query.getForce()), result);
                if (Boolean.TRUE.equals(result.getConflict()) && !Boolean.TRUE.equals(query.getForce())) {
                    return result;
                }
                // 按号码匹配公海档案（命中取 create_time 最新；未命中按勾选决定是否建档，建档在监听器内）
                Map<String, CustomerSeas> matched = matchCustomersByPhone(pending);
                for (JSONObject row : rows) {
                    String phone = row.getString("phone");
                    if (StringUtils.isBlank(phone) || !pending.contains(phone.trim())) {
                        continue;
                    }
                    CustomerSeas matchedCustomer = matched.get(phone.trim());
                    applicationEventPublisher.publishEvent(new CallTaskContactImportEvent(
                            Objects.nonNull(matchedCustomer) ? matchedCustomer.getId() : null,
                            query.getTaskId(), query.getImportType(), null, query.getTemplateId(), row,
                            query.getCreateCustomer()));
                    result.setImported(result.getImported() + 1);
                }
            }
        }
        return result;
    }

    /**
     * 导入预检（设计 D11/D12）：任务内同 phone 跳过并计数；跨"进行中"任务重复与号码已归属
     * 坐席私海的明细列出，未 force 时置 conflict 不导入（已暂停/已结束任务不提示——不同活动复打合法）。
     *
     * @return 本次可导入的 phone 集合（任务内已存在的被剔除）
     */
    private Set<String> precheckImport(Long taskId, List<String> phones, boolean force, CallTaskContactImportResultVo result) {
        Set<String> unique = new LinkedHashSet<>();
        for (String phone : phones) {
            if (StringUtils.isNotBlank(phone)) {
                unique.add(phone.trim());
            }
        }
        if (unique.isEmpty()) {
            return unique;
        }
        // 任务内已有：跳过（应用层防重；软删模式下不建唯一索引，见设计 D13）
        Set<String> existing = callTaskAssignmentService.list(new LambdaQueryWrapper<CallTaskAssignment>()
                        .eq(CallTaskAssignment::getTaskId, taskId)
                        .in(CallTaskAssignment::getPhone, unique)
                        .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()))
                .stream().map(CallTaskAssignment::getPhone).collect(Collectors.toSet());
        Set<String> pending = unique.stream().filter(phone -> !existing.contains(phone)).collect(Collectors.toSet());
        result.setSkipped(unique.size() - pending.size());
        if (pending.isEmpty() || force) {
            return pending;
        }
        // 跨进行中任务重复
        List<CallTaskAssignment> crossTask = callTaskAssignmentService.list(new LambdaQueryWrapper<CallTaskAssignment>()
                .ne(CallTaskAssignment::getTaskId, taskId)
                .in(CallTaskAssignment::getPhone, pending)
                .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        if (!crossTask.isEmpty()) {
            Set<Long> taskIds = crossTask.stream().map(CallTaskAssignment::getTaskId).collect(Collectors.toSet());
            Map<Long, String> processingNames = listByIds(taskIds).stream()
                    .filter(t -> Objects.equals(t.getStatus(), CallTaskStatusEnum.PROCESSING.getCode()))
                    .collect(Collectors.toMap(CallTask::getId, CallTask::getName));
            crossTask.stream()
                    .filter(a -> processingNames.containsKey(a.getTaskId()))
                    .collect(Collectors.groupingBy(CallTaskAssignment::getPhone))
                    .forEach((phone, list) -> {
                        String names = list.stream().map(a -> processingNames.get(a.getTaskId()))
                                .distinct().collect(Collectors.joining("、"));
                        result.addConflictDetail("号码 " + phone + " 已在进行中任务《" + names + "》");
                    });
        }
        // 号码已归属坐席私海：提示可继续（通知型场景留口；人群互斥语义由人群圈选侧保证）
        customerSeasService.list(new LambdaQueryWrapper<CustomerSeas>()
                        .in(CustomerSeas::getPhone, pending)
                        .isNotNull(CustomerSeas::getOwnerId)
                        .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()))
                .forEach(c -> result.addConflictDetail("号码 " + c.getPhone() + " 已是坐席私海客户（确认继续为通知型场景）"));
        return pending;
    }

    /**
     * 按号码匹配公海客户档案（Excel 导入关联用）：命中多条取 create_time 最新，歧义不阻断
     */
    private Map<String, CustomerSeas> matchCustomersByPhone(Collection<String> phones) {
        if (CollectionUtil.isEmpty(phones)) {
            return Collections.emptyMap();
        }
        Map<String, CustomerSeas> matched = new HashMap<>();
        // 按 create_time 倒序后 putIfAbsent，首个即最新
        customerSeasService.list(new LambdaQueryWrapper<CustomerSeas>()
                        .in(CustomerSeas::getPhone, phones)
                        .eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                        .orderByDesc(BaseEntity::getCreateTime))
                .forEach(c -> matched.putIfAbsent(c.getPhone(), c));
        return matched;
    }

    @Override
    public List<CallTaskContactVo> getMyTaskContactPageList(CallTaskContactQuery query, Long userId) {
        Long agentId = getAgentIdByUserId(userId);
        if (Objects.isNull(agentId)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        // 服务端覆写过滤条件：仅本人已分配，防越权（前端传 agentIds 无效）
        query.setAgentIds(Collections.singletonList(agentId));
        query.setStatus(1);
        // 坐席视角跨任务查，无 taskId，不复用 getTaskContactPageList（其必填 taskId 校验仅约束任务视角）
        super.startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return getTaskContactList(query);
    }

    @Override
    public List<CallTaskMySummaryVo> getMyTaskSummaryList(Long userId) {
        Long agentId = getAgentIdByUserId(userId);
        if (Objects.isNull(agentId)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        return this.baseMapper.getMyTaskSummary(agentId);
    }

    @Override
    public String dialTaskContact(Long assignmentId, Long userId) {
        if (Objects.isNull(assignmentId)) {
            throw new CommonException("联系人ID不能为空");
        }
        CallTaskAssignment assignment = callTaskAssignmentService.getById(assignmentId);
        if (Objects.isNull(assignment)) {
            throw new CommonException("无效ID");
        }
        Long agentId = getAgentIdByUserId(userId);
        if (Objects.isNull(agentId)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        if (!Objects.equals(assignment.getAgentId(), agentId)) {
            throw new CommonException("该联系人未分配给当前坐席");
        }
        // 任务须为进行中：暂停/结束任务的名单不可拨（工作台任务列表仅显示进行中任务，此处后端兜底防绕过）
        CallTask task = getById(assignment.getTaskId());
        if (Objects.isNull(task) || !Objects.equals(task.getStatus(), CallTaskStatusEnum.PROCESSING.getCode())) {
            throw new CommonException("任务未在进行中，无法拨打");
        }
        // 已拨打可重拨（不校验 callStatus，前端放开置灰）；归属校验 + 返回被叫号码——
        // 身份标识由前端随呼叫携带（软电话 SIP 头 / 座机代拨入参），挂断回写按 CallInfo 直关联，
        // 本接口不再计数不再写 Redis 映射（原 task:dialing 键机制已整体退役，attempt_count 挪到挂断回写）
        return assignment.getPhone();
    }

    @Override
    public List<CallTaskDialLogVo> getDialLogPageList(CallTaskDialLogQuery query) {
        if (Objects.isNull(query.getAssignmentId()) && Objects.isNull(query.getCustomerId()) && Objects.isNull(query.getTaskId())) {
            throw new CommonException("查询维度不能为空（联系人/客户/任务任传其一）");
        }
        super.startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return this.baseMapper.getDialLogList(query);
    }

    @Override
    public void submitDisposition(CallTaskDispositionQuery query, Long userId) {
        if (Objects.isNull(query.getAssignmentId()) && Objects.isNull(query.getCustomerId())) {
            throw new CommonException("定位维度不能为空（联系人/客户任传其一）");
        }
        Long agentId = getAgentIdByUserId(userId);
        if (Objects.isNull(agentId)) {
            throw new CommonException("当前用户未绑定坐席账号");
        }
        // 前端挂断时拿不到异步落库的 dialLogId，按拨打维度取最近一条定位；
        // 极端时序（回写慢于坐席提交）查不到记录，提示稍后重试（量小可接受）
        LambdaQueryWrapper<CallTaskDialLog> wrapper = new LambdaQueryWrapper<>();
        if (Objects.nonNull(query.getAssignmentId())) {
            wrapper.eq(CallTaskDialLog::getAssignmentId, query.getAssignmentId());
        } else {
            wrapper.eq(CallTaskDialLog::getCustomerId, query.getCustomerId());
        }
        wrapper.eq(CallTaskDialLog::getAgentId, agentId)
                .orderByDesc(CallTaskDialLog::getDialTime)
                .last("limit 1");
        CallTaskDialLog dialLog = callTaskDialLogService.getOne(wrapper);
        if (Objects.isNull(dialLog)) {
            throw new CommonException("通话记录尚未生成，请稍后重试");
        }
        CallTaskDialLog update = new CallTaskDialLog();
        update.setId(dialLog.getId());
        update.setDisposition(query.getDisposition());
        update.setDispositionRemark(query.getDispositionRemark());
        callTaskDialLogService.updateById(update);
    }

    @Override
    public List<TaskProgressVo> getTaskProgress() {
        List<TaskProgressVo> list = baseMapper.getTaskProgress();
        for (TaskProgressVo vo : list) {
            long total = Objects.isNull(vo.getTotalNum()) ? 0 : vo.getTotalNum();
            long dialed = Objects.isNull(vo.getDialedNum()) ? 0 : vo.getDialedNum();
            if (total > 0) {
                vo.setProgress(new BigDecimal(dialed * 100L).divide(new BigDecimal(total), 1, RoundingMode.HALF_UP));
            } else {
                vo.setProgress(BigDecimal.ZERO);
            }
        }
        return list;
    }

    /**
     * 登录用户绑定的坐席ID（统一入口 ISipAgentService.getAgentIdByUserId，未绑定返回 null）
     */
    private Long getAgentIdByUserId(Long userId) {
        return sipAgentService.getAgentIdByUserId(userId);
    }


    private Boolean checkName(String name) {
        long count = count(new LambdaQueryWrapper<CallTask>().eq(CallTask::getName, name).eq(BaseEntity::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex()));
        return count > 0;
    }
}

