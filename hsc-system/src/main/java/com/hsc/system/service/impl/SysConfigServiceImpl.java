package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.constant.SysConfigKeys;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.system.domain.entity.SysConfig;
import com.hsc.system.domain.query.config.SysConfigEditQuery;
import com.hsc.system.domain.query.config.SysConfigQuery;
import com.hsc.system.domain.vo.config.SysConfigVo;
import com.hsc.system.mapper.SysConfigMapper;
import com.hsc.system.service.ISysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 系统参数表(SysConfig)。缓存策略：本地全量 KV 缓存(volatile 整体替换)——
 * 这些值在 ESL 事件高频路径(每通电话 originate 都要读振铃超时)，内存读取零开销；
 * 当前单实例部署无跨实例一致性问题(多实例部署需在 edit 后追加失效广播)。
 * edit 是唯一改值入口，保存成功后 reloadCache() 刷新本 JVM，实时生效无需重启。
 *
 * @author pangshuai
 * @date 2026-09-01
 */
@Service
@Slf4j
public class SysConfigServiceImpl extends BaseServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    /**
     * 整型参数值白名单：key -> {min, max}。edit 按此校验，防止把超时改 0 秒、
     * token 改 5 分钟这类运营事故值写入库。仅预置参数可改(白名单外一律拒绝)。
     */
    private static final Map<String, long[]> INT_RANGE = Map.ofEntries(
            Map.entry(SysConfigKeys.RING_TIMEOUT_OUTBOUND, new long[]{5, 300}),
            Map.entry(SysConfigKeys.RING_TIMEOUT_INBOUND_AGENT, new long[]{5, 300}),
            Map.entry(SysConfigKeys.RING_TIMEOUT_INBOUND_EXTENSION, new long[]{5, 300}),
            Map.entry(SysConfigKeys.DESK_DIAL_AGENT_RING_TIMEOUT, new long[]{5, 300}),
            Map.entry(SysConfigKeys.QUEUE_TIMEOUT_DEFAULT, new long[]{5, 300}),
            Map.entry(SysConfigKeys.QUEUE_CAPACITY_DEFAULT, new long[]{1, 10000}),
            Map.entry(SysConfigKeys.MISSED_REASSIGN_LIMIT, new long[]{0, 10}),
            Map.entry(SysConfigKeys.QUEUE_ANNOUNCE_INTERVAL_MS, new long[]{5000, 300000}),
            Map.entry(SysConfigKeys.TOKEN_EXPIRE_MINUTES, new long[]{30, 43200}));

    /** 本地全量缓存 key->value；volatile 引用整体替换，读侧无锁 */
    private volatile Map<String, String> cache = Map.of();

    @PostConstruct
    public void loadCache() {
        reloadCache();
    }

    private void reloadCache() {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex());
        List<SysConfig> list = this.list(wrapper);
        Map<String, String> fresh = new HashMap<>(list.size());
        for (SysConfig item : list) {
            fresh.put(item.getConfigKey(), item.getConfigValue());
        }
        this.cache = Map.copyOf(fresh);
        log.info("sys_config 缓存已加载: {} 条", fresh.size());
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = cache.get(key);
        if (Objects.isNull(value) || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("sys_config 参数[{}]值[{}]非整型,回退默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public PageInfo<SysConfigVo> pageList(SysConfigQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getDelFlag, DeleteStatusEnum.DELETE_NO.getIndex())
                .like(Objects.nonNull(query.getConfigName()) && !query.getConfigName().isBlank(), SysConfig::getConfigName, query.getConfigName())
                .like(Objects.nonNull(query.getConfigKey()) && !query.getConfigKey().isBlank(), SysConfig::getConfigKey, query.getConfigKey())
                .orderByAsc(SysConfig::getId);
        List<SysConfig> list = this.list(wrapper);
        PageInfo<SysConfig> page = new PageInfo<>(list);
        PageInfo<SysConfigVo> result = new PageInfo<>();
        result.setList(list.stream().map(this::toVo).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getPageNum());
        result.setPageSize(page.getPageSize());
        return result;
    }

    @Override
    public void edit(SysConfigEditQuery query) {
        SysConfig exist = this.getById(query.getId());
        if (Objects.isNull(exist)
                || Objects.equals(exist.getDelFlag(), DeleteStatusEnum.DELETE_YES.getIndex())) {
            throw new CommonException("参数不存在");
        }
        validateValue(exist.getConfigKey(), query.getConfigValue());

        SysConfig update = new SysConfig();
        update.setId(exist.getId());
        // 仅值与备注可改：不 set configKey/configName，请求即便携带也无字段承接
        update.setConfigValue(query.getConfigValue().trim());
        update.setRemark(query.getRemark());
        this.updateById(update);
        // 实时生效：保存成功后立即重建本 JVM 缓存。
        // DB 已改而刷新失败(瞬时抖动)时不能向用户报"保存失败"误导重试方向——吞掉只留 error 日志，
        // 缓存停留旧值至下一次 edit/重启；这正是"改了没生效"的自愈出口(下次任何一次 edit 都会全量重建)
        try {
            reloadCache();
        } catch (Exception e) {
            log.error("sys_config 已保存但缓存刷新失败,当前读取仍为旧值,将在下次修改或重启后恢复: {}", exist.getConfigKey(), e);
        }
        log.info("sys_config 参数已修改并生效: {} = {}", exist.getConfigKey(), query.getConfigValue());
    }

    /**
     * 按白名单校验参数值：整型查范围；白名单外的 key(非预置脏数据)直接拒绝编辑
     */
    private void validateValue(String configKey, String value) {
        long[] range = INT_RANGE.get(configKey);
        if (Objects.nonNull(range)) {
            long parsed;
            try {
                parsed = Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                throw new CommonException("参数值必须为整数");
            }
            if (parsed < range[0] || parsed > range[1]) {
                throw new CommonException(String.format("参数值超出允许范围: %d ~ %d", range[0], range[1]));
            }
            return;
        }
        throw new CommonException("该参数非系统预置参数,不支持修改");
    }

    private SysConfigVo toVo(SysConfig entity) {
        SysConfigVo vo = new SysConfigVo();
        vo.setId(entity.getId());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigName(entity.getConfigName());
        vo.setConfigValue(entity.getConfigValue());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
