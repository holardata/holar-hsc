// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service;


import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.FsConfig;
import com.hsc.system.domain.query.fsconfig.FsConfigAddQuery;
import com.hsc.system.domain.query.fsconfig.FsConfigQuery;

import java.util.List;

/**
 * fs管理配置表(FsConfig)表服务接口
 *
 * @author danmo
 * @since 2023-10-17 11:04:58
 */
public interface IFsConfigService extends IBaseService<FsConfig> {

    void add(FsConfigAddQuery query);

    void edit(FsConfigAddQuery query);

    FsConfig getDetail(Integer id);

    void delete(FsConfigQuery query);

    List<FsConfig> getList(FsConfigQuery query);
    List<FsConfig> getPageList(FsConfigQuery query);
}

