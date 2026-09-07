package com.hsc.system.service;


import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallDisplayPool;
import com.hsc.system.domain.query.display.CallDisplayPoolAddQuery;
import com.hsc.system.domain.query.display.CallDisplayPoolQuery;
import com.hsc.system.domain.vo.display.CallDisplayPoolVo;

import java.util.List;

/**
 * 号码池管理(CallDisplayPool)表服务接口
 *
 * @author danmo
 * @since 2024-08-09 14:57:16
 */
public interface ICallDisplayPoolService extends IBaseService<CallDisplayPool> {

    void addPool(CallDisplayPoolAddQuery query);

    void editPool(CallDisplayPoolAddQuery query);

    void deletePool(CallDisplayPoolQuery query);

    CallDisplayPoolVo getPoolDetail(Long id);

    PageInfo<CallDisplayPoolVo> pagePoolList(CallDisplayPoolQuery query);

    List<CallDisplayPoolVo> getPoolList(CallDisplayPoolQuery query);
}

