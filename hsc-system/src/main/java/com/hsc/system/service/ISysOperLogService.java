package com.hsc.system.service;

import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysOperLog;
import com.hsc.system.domain.query.BaseQuery;
import com.hsc.system.domain.query.log.SysOperationLogQuery;

import java.util.List;

/**
 * 系统操作日志记录(SysOperLog)表服务接口
 *
 * @author danmo
 * @since 2024-03-12 19:09:58
 */
public interface ISysOperLogService extends IBaseService<SysOperLog> {

    List<SysOperLog> getList(SysOperationLogQuery query);

    void delete(SysOperationLogQuery query);

    void emptyOperation(BaseQuery query);
}

