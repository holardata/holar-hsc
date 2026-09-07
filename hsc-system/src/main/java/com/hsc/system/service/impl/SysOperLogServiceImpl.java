// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.SysOperLog;
import com.hsc.system.domain.query.BaseQuery;
import com.hsc.system.domain.query.log.SysOperationLogQuery;
import com.hsc.system.mapper.SysOperLogMapper;
import com.hsc.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统操作日志记录(SysOperLog)表服务实现类
 *
 * @author danmo
 * @since 2024-03-12 19:09:58
 */
@Service
public class SysOperLogServiceImpl extends BaseServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    @Override
    public List<SysOperLog> getList(SysOperationLogQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        return this.baseMapper.getList(query);
    }

    @Override
    public void delete(SysOperationLogQuery query) {
        if (CollectionUtil.isEmpty(query.getIds())) {
            throw new CommonException("ID不能为空");
        }
        List<SysOperLog> operLogs = query.getIds().stream().map(id -> {
            SysOperLog operLog = new SysOperLog();
            operLog.setDelFlag(DeleteStatusEnum.DELETE_YES.getIndex());
            operLog.setId(id);
            return operLog;
        }).collect(Collectors.toList());
        updateBatchById(operLogs,200);

    }

    @Override
    public void emptyOperation(BaseQuery query) {
        remove(new LambdaQueryWrapper<SysOperLog>().gt(SysOperLog::getId,0));
    }
}

