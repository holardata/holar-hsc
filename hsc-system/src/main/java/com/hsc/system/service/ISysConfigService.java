package com.hsc.system.service;

import com.github.pagehelper.PageInfo;
import com.hsc.common.base.IBaseService;
import com.hsc.system.domain.entity.SysConfig;
import com.hsc.system.domain.query.config.SysConfigEditQuery;
import com.hsc.system.domain.query.config.SysConfigQuery;
import com.hsc.system.domain.vo.config.SysConfigVo;

/**
 * 系统参数表(SysConfig)：参数由系统预置不可增删，仅参数值与备注可改；
 * 业务代码运行时经本服务的 getXxx(key, default) 统一入口读取（本地缓存，
 * 保存后即刻刷新，无需重启实时生效；无记录返回代码默认值）。
 *
 * @author pangshuai
 * @date 2026-09-01
 */
public interface ISysConfigService extends IBaseService<SysConfig> {

    /**
     * 读整型参数；无记录/值非法时返回 defaultValue（warn 日志留痕）
     */
    int getInt(String key, int defaultValue);

    /**
     * 分页查询参数列表（按参数名/键名模糊过滤）
     */
    PageInfo<SysConfigVo> pageList(SysConfigQuery query);

    /**
     * 修改参数值与备注（按 id 反查，键名/参数名不可改；按白名单校验值范围，
     * 保存成功后刷新本地缓存实时生效）
     */
    void edit(SysConfigEditQuery query);
}
