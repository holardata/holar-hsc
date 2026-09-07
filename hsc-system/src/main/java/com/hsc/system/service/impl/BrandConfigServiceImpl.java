package com.hsc.system.service.impl;

import com.hsc.common.base.BaseServiceImpl;
import com.hsc.system.domain.entity.SiteConfig;
import com.hsc.system.domain.query.brand.BrandConfigQuery;
import com.hsc.system.domain.vo.brand.BrandConfigVo;
import com.hsc.system.mapper.SiteConfigMapper;
import com.hsc.system.service.IBrandConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 品牌配置(site_config)表服务实现。KV 键值对读取与整段保存，
 * 键集与 {@link BrandConfigVo} 字段一一对应；新增品牌键 = VO/Query 加字段 + 此处映射加一行。
 */
@Slf4j
@Service
public class BrandConfigServiceImpl
        extends BaseServiceImpl<SiteConfigMapper, SiteConfig>
        implements IBrandConfigService {

    private static final String KEY_APP_NAME = "appName";
    private static final String KEY_LOGO_FILE_ID = "logoFileId";
    private static final String KEY_FAVICON_FILE_ID = "faviconFileId";
    private static final String KEY_SLOGAN_IMAGE_FILE_ID = "sloganImageFileId";
    private static final String KEY_PAGE_TITLE = "pageTitle";
    private static final String KEY_PAGE_DESCRIPTION = "pageDescription";
    private static final String KEY_LOGIN_SUBTITLE = "loginSubtitle";
    private static final String KEY_DOC_URL = "docUrl";
    private static final String KEY_DOC_URL_ENABLED = "docUrlEnabled";

    @Override
    public BrandConfigVo getBrandConfig() {
        Map<String, String> kv = loadKv();
        BrandConfigVo vo = new BrandConfigVo();
        vo.setAppName(kv.getOrDefault(KEY_APP_NAME, ""));
        vo.setLogoFileId(kv.getOrDefault(KEY_LOGO_FILE_ID, ""));
        vo.setFaviconFileId(kv.getOrDefault(KEY_FAVICON_FILE_ID, ""));
        vo.setSloganImageFileId(kv.getOrDefault(KEY_SLOGAN_IMAGE_FILE_ID, ""));
        vo.setPageTitle(kv.getOrDefault(KEY_PAGE_TITLE, ""));
        vo.setPageDescription(kv.getOrDefault(KEY_PAGE_DESCRIPTION, ""));
        vo.setLoginSubtitle(kv.getOrDefault(KEY_LOGIN_SUBTITLE, ""));
        vo.setDocUrl(kv.getOrDefault(KEY_DOC_URL, ""));
        // 文档入口开关：KV 存 "true"/"false"，未配置视为开（链接非空即显示）
        vo.setDocUrlEnabled(!"false".equals(kv.getOrDefault(KEY_DOC_URL_ENABLED, "true")));
        return vo;
    }

    @Override
    public void saveBrandConfig(BrandConfigQuery query) {
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put(KEY_APP_NAME, nvl(query.getAppName()));
        kv.put(KEY_LOGO_FILE_ID, nvl(query.getLogoFileId()));
        kv.put(KEY_FAVICON_FILE_ID, nvl(query.getFaviconFileId()));
        kv.put(KEY_SLOGAN_IMAGE_FILE_ID, nvl(query.getSloganImageFileId()));
        kv.put(KEY_PAGE_TITLE, nvl(query.getPageTitle()));
        kv.put(KEY_PAGE_DESCRIPTION, nvl(query.getPageDescription()));
        kv.put(KEY_LOGIN_SUBTITLE, nvl(query.getLoginSubtitle()));
        kv.put(KEY_DOC_URL, nvl(query.getDocUrl()));
        // null 视为开（与"未配置=默认开"语义一致，"恢复默认"整段保存不清开关）
        kv.put(KEY_DOC_URL_ENABLED, String.valueOf(!Objects.equals(Boolean.FALSE, query.getDocUrlEnabled())));

        // 一次查全现有键再比对，按键 upsert；值未变化的键跳过写库
        Map<String, SiteConfig> existing = list().stream()
                .collect(Collectors.toMap(SiteConfig::getConfigKey, Function.identity(), (a, b) -> a));
        kv.forEach((key, value) -> {
            SiteConfig cfg = existing.get(key);
            if (cfg == null) {
                cfg = new SiteConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(value);
                save(cfg);
            } else if (!Objects.equals(cfg.getConfigValue(), value)) {
                cfg.setConfigValue(value);
                updateById(cfg);
            }
        });
        log.info("品牌配置已保存: {}", kv);
    }

    /** 全表 KV 读取（键值 null 安全）。 */
    private Map<String, String> loadKv() {
        return list().stream().collect(Collectors.toMap(
                SiteConfig::getConfigKey,
                c -> nvl(c.getConfigValue()),
                (a, b) -> a));
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
