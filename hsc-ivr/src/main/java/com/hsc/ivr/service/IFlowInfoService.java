package com.hsc.ivr.service;

import com.hsc.common.base.IBaseService;
import com.hsc.ivr.domain.entity.FlowInfo;
import com.hsc.ivr.domain.query.FlowInfoAddQuery;
import com.hsc.ivr.domain.query.FlowInfoQuery;
import com.hsc.ivr.domain.vo.FlowInfoListVo;
import com.hsc.ivr.domain.vo.FlowInfoVo;

import java.util.List;

/**
 * ivr流程信息(FlowInfo)表服务接口
 *
 * @author danmo
 * @since 2024-12-23 15:08:24
 */
public interface IFlowInfoService extends IBaseService<FlowInfo> {

    void add(FlowInfoAddQuery query);

    void edit(FlowInfoAddQuery query);

    void delete(Long id);

    FlowInfoVo getInfo(Long id);

    List<FlowInfoListVo> pageList(FlowInfoQuery query);

    void publish(Long id);

    void offline(Long id);

}

