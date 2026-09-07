// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.calltask.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hsc.calltask.domain.CustomerCrowdEvent;
import com.hsc.calltask.domain.CustomerCrowdEventParam;
import com.hsc.calltask.domain.entity.CustomerPoolLog;
import com.hsc.calltask.domain.entity.CustomerSeas;
import com.hsc.calltask.domain.query.CustomerSeasAddQuery;
import com.hsc.calltask.domain.query.CustomerSeasQuery;
import com.hsc.calltask.domain.vo.CustomerSeasVo;
import com.hsc.calltask.domain.vo.CustomerTemplateFieldRelVo;
import com.hsc.calltask.domain.vo.CustomerTemplateVo;
import com.hsc.calltask.mapper.CustomerSeasMapper;
import com.hsc.calltask.service.ICustomerPoolLogService;
import com.hsc.calltask.service.ICustomerSeasService;
import com.hsc.calltask.service.ICustomerTemplateService;
import com.hsc.calltask.util.FieldHeaders;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.CustomerSourceEnum;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.enums.FieldTypeEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户公海表(CustomerSeas)表服务实现类
 *
 * @author danmo
 * @since 2025-06-27 14:13:06
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class CustomerSeasServiceImpl extends BaseServiceImpl<CustomerSeasMapper, CustomerSeas> implements ICustomerSeasService {

    private final ISysUserService sysUserService;
    private final ICustomerTemplateService customerTemplateService;
    private final ApplicationContext applicationContext;
    private final ICustomerPoolLogService customerPoolLogService;

    @Override
    public void add(CustomerSeasAddQuery query) {
        CustomerSeas customerSeas = new CustomerSeas();
        BeanUtils.copyProperties(query, customerSeas);
        customerSeas.setSource(CustomerSourceEnum.MANUAL.getCode());
        if(save(customerSeas)){
            saveImportPoolLog(customerSeas.getId());
            CustomerCrowdEventParam param = new CustomerCrowdEventParam();
            param.setCustomerId(customerSeas.getId());
            param.setEventType(1);
            applicationContext.publishEvent(new CustomerCrowdEvent(param));
        }
    }

    @Override
    public void edit(CustomerSeasAddQuery query) {
        CustomerSeas customerSeas = getById(query.getId());
        if (Objects.isNull(customerSeas)) {
            throw new CommonException("无效ID");
        }
        BeanUtils.copyProperties(query, customerSeas);
        if(updateById(customerSeas)){
            // 资料变更后重评估自动人群归属，不依赖每日 Job 兜底
            CustomerCrowdEventParam param = new CustomerCrowdEventParam();
            param.setCustomerId(customerSeas.getId());
            param.setEventType(1);
            applicationContext.publishEvent(new CustomerCrowdEvent(param));
        }
    }

    @Override
    public CustomerSeasVo getDetail(Long id) {
        return this.baseMapper.getDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(CustomerSeasQuery query) {
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
        List<CustomerSeas> list = ids.stream().map(id -> {
            CustomerSeas seas = new CustomerSeas();
            seas.setId(id);
            seas.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            return seas;
        }).toList();
        if(updateBatchById(list)){
            for (Long id : ids) {
                CustomerCrowdEventParam param = new CustomerCrowdEventParam();
                param.setCustomerId(id);
                param.setEventType(2);
                applicationContext.publishEvent(new CustomerCrowdEvent(param));
            }
        }


    }

    @Override
    public List<CustomerSeasVo> pageList(CustomerSeasQuery query) {
        super.startPage(query.getPageIndex(), query.getPageSize());
        List<CustomerSeasVo> customerSeas =getList(query);
        if(!CollectionUtil.isEmpty(customerSeas)){
            sysUserService.decorate(customerSeas);
        }
        return customerSeas;
    }

    @Override
    public List<CustomerSeasVo> getList(CustomerSeasQuery query) {
        return this.baseMapper.getList(query);
    }

    @Transactional(rollbackFor = {Exception.class,CommonException.class})
    @Override
    public void importCustomer(Long templateId, MultipartFile file) {
        CustomerTemplateVo customerTemplate = customerTemplateService.getDetail(templateId);
        if(Objects.isNull(customerTemplate)){
            throw new CommonException("无效的模板ID");
        }
        if(CollectionUtils.isEmpty(customerTemplate.getFieldList())){
            throw new CommonException("模板未添加字段");
        }
        //必填字段
        Map<String, Boolean> requiredFieldMap = customerTemplate.getFieldList().stream().collect(Collectors.toMap(CustomerTemplateFieldRelVo::getFieldName, item -> item.getRequired() == 1, (key1, key2) -> key1));
        //字段类型
        Map<String, Integer> fieldTypeMap = customerTemplate.getFieldList().stream().collect(Collectors.toMap(CustomerTemplateFieldRelVo::getFieldName, CustomerTemplateFieldRelVo::getFieldType, (key1, key2) -> key1));
        //单选/多选选项存储值映射(fieldName -> (存储值 -> 显示名))：导入值须命中存储值，否则列表无法翻译成显示名
        Map<String, Map<String, String>> optionValueMap = customerTemplate.getFieldList().stream()
                .filter(item -> Objects.equals(item.getFieldType(), FieldTypeEnum.SINGLE_CHOICE.getCode()) || Objects.equals(item.getFieldType(), FieldTypeEnum.MULTI_CHOICE.getCode()))
                .collect(Collectors.toMap(CustomerTemplateFieldRelVo::getFieldName, item -> parseOptionValues(item.getOptions()), (key1, key2) -> key1));
        try {
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                private Map<Integer, String> headMap;
                private static final int BATCH_COUNT = 100;
                private List<CustomerSeas> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    this.headMap = headMap;
                }

                @Override
                public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                    if (headMap == null) return; // 确保表头已读取
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                        Integer colIndex = entry.getKey();
                        String headerName = headMap.get(colIndex);
                        String cellValue = entry.getValue();
                        //从表头"显示名(fieldName)"解析字段标识(与任务联系人导入共用)
                        headerName = FieldHeaders.fieldNameOf(headerName);
                        if(requiredFieldMap.get(headerName) && StringUtils.isEmpty(cellValue)){
                            throw new CommonException("字段" + headerName + "不能为空");
                        }
                        //非必填字段留空不校验格式(空串过不了电话等正则)
                        if(StringUtils.isNotEmpty(cellValue)){
                            String checkFormat = FieldTypeEnum.checkFormat(fieldTypeMap.get(headerName), cellValue);
                            if(StringUtils.isNotEmpty(checkFormat)){
                                throw new CommonException("字段" + headerName + "," + cellValue + "格式错误," + checkFormat);
                            }
                            //单选/多选值(多选拆逗号逐个)须命中选项存储值，否则前端列表无法翻译成显示名
                            Map<String, String> legalOptions = optionValueMap.get(headerName);
                            if(legalOptions != null){
                                for(String v : cellValue.split(",")){
                                    if(!legalOptions.containsKey(v.trim())){
                                        String legal = legalOptions.entrySet().stream().map(e -> e.getValue() + "=" + e.getKey()).collect(Collectors.joining("、"));
                                        throw new CommonException("字段" + headerName + "的值\"" + v.trim() + "\"不在选项范围内(允许:" + legal + ")");
                                    }
                                }
                            }
                        }
                        rowMap.put(headerName, cellValue);
                    }
                    CustomerSeas customerSeas = new CustomerSeas();
                    customerSeas.setTemplateId(templateId);
                    customerSeas.setSource(CustomerSourceEnum.FILE_IMPORT.getCode());
                    customerSeas.setCustomerInfo(JSON.toJSONString(rowMap));
                    cachedDataList.add(customerSeas);
                    if (cachedDataList.size() >= BATCH_COUNT) {
                        saveData();
                        // 存储完成清理 list
                        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 这里也要保存数据，确保最后遗留的数据也存储到数据库
                    saveData();
                    log.info("所有数据解析完成！");
                }

                private void saveData() {
                    log.info("{}条数据，开始存储数据库！", cachedDataList.size());
                    if(saveBatch(cachedDataList)){
                        for (CustomerSeas customerSeas : cachedDataList) {
                            saveImportPoolLog(customerSeas.getId());
                            CustomerCrowdEventParam param = new CustomerCrowdEventParam();
                            param.setCustomerId(customerSeas.getId());
                            param.setEventType(1);
                            applicationContext.publishEvent(new CustomerCrowdEvent(param));
                        }
                    };
                    log.info("存储数据库成功！");
                }
            }).sheet().doRead();
        } catch (CommonException e) {
            throw e;
        } catch (ExcelAnalysisException e) {
            // EasyExcel 会把解析回调(invoke)里抛出的业务异常包装一层，需解包透传给用户
            Throwable cause = e.getCause();
            if (cause instanceof CommonException ce) {
                throw ce;
            }
            log.error("客户导入解析失败", e);
            throw new CommonException("导入失败:" + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (Exception e) {
            log.error("客户导入失败", e);
            throw new CommonException("导入失败:" + e.getMessage());
        }

    }

    /**
     * 客户入库记流转日志（action=1 导入，operator 空=导入事件无操作人，时间线起点）
     */
    private void saveImportPoolLog(Long customerId) {
        CustomerPoolLog poolLog = new CustomerPoolLog();
        poolLog.setCustomerId(customerId);
        poolLog.setAction(1);
        poolLog.setCreateTime(new Date());
        customerPoolLogService.save(poolLog);
    }

    /**
     * 解析字段 options JSON([{key显示名,value存储值,visible}])为 存储值->显示名 映射，
     * 供导入时校验单选/多选值在选项范围内
     */
    private Map<String, String> parseOptionValues(String options) {
        Map<String, String> map = new LinkedHashMap<>();
        if(StringUtils.isBlank(options)){
            return map;
        }
        JSONArray arr = JSON.parseArray(options);
        for(int i = 0; i < arr.size(); i++){
            JSONObject opt = arr.getJSONObject(i);
            if(StringUtils.isNotEmpty(opt.getString("value"))){
                map.put(opt.getString("value"), opt.getString("key"));
            }
        }
        return map;
    }
}

