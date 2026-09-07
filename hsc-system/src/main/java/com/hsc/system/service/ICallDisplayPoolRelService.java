// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.CallDisplayPoolRel;

import java.util.List;

/**
 * 号码池号码关联表(CallDisplayPoolRel)表服务接口
 *
 * @author danmo
 * @since 2024-08-09 14:58:05
 */
public interface ICallDisplayPoolRelService extends IBaseService<CallDisplayPoolRel> {

    void addByPoolId(Long poolId, List<Long> phoneList);

    void editByPoolId(Long poolId, List<Long> phoneList);

    void delByPoolId(Long poolId);
}

