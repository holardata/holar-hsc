package com.hsc.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsc.system.domain.entity.SiteConfig;
import com.hsc.system.domain.query.brand.BrandConfigQuery;
import com.hsc.system.domain.vo.brand.BrandConfigVo;

/**
 * 品牌配置(site_config)服务。OEM 品牌要素的读取与整段保存。
 *
 * <p>读取供前端启动/登录页（免鉴权接口）拉取品牌；保存供品牌设置页。
 * 空值语义：config_value 空串（或键不存在）= 前端回退构建默认值，
 * 全部传空保存即"一键恢复默认"。
 */
public interface IBrandConfigService extends IService<SiteConfig> {

    /**
     * 读取品牌配置（未配置的键返回空串，不返回 null）。
     */
    BrandConfigVo getBrandConfig();

    /**
     * 整段保存品牌配置（按键 upsert，null 入参按空串落库）。
     */
    void saveBrandConfig(BrandConfigQuery query);
}
