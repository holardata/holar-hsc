// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.utils.PhoneUtils;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.HscRegion;
import com.hsc.system.mapper.PhoneLocationMapper;
import com.hsc.system.domain.entity.PhoneLocation;
import com.hsc.system.service.IHscRegionService;
import com.hsc.system.service.IPhoneLocationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * (PhoneLocation)表服务实现类
 *
 * @author danmo
 * @since 2025-05-26 17:12:25
 */
@AllArgsConstructor
@Service
public class PhoneLocationServiceImpl extends BaseServiceImpl<PhoneLocationMapper, PhoneLocation> implements IPhoneLocationService {

    private final IHscRegionService ochRegionService;

    @Override
    public String getLocation(String phone) {
        String phoneArea = PhoneUtils.getPhoneArea(phone);
        if (phoneArea != null){
            PhoneLocation phoneLocation = this.getOne(new LambdaQueryWrapper<PhoneLocation>()
                    .eq(PhoneLocation::getPhone, phoneArea).last("limit 1"));
            if (phoneLocation != null){
                HscRegion ochRegion = ochRegionService.getOne(new LambdaQueryWrapper<HscRegion>().eq(HscRegion::getRegionId, phoneLocation.getAreaCode()).last("limit 1"));
                if (ochRegion != null){
                    return ochRegion.getMergerName();
                }
            }
        }
        return getLandlineLocation(phone);
    }

    /**
     * 座机号按区号解析归属地（显号管理的外显号多为固话）：region 表市级city_code 匹配。
     * 必须 level=2 限定——同城所有区县 city_code 相同，不限会命中多条。先试 3 位再试
     * 4 位区号无歧义（3 位区号仅 010/02x，4 位区号前 3 位均不构成区号）；400/800 等
     * 非 0 开头、以及清洗后过短的号码返回空。输入带"-"等分隔符先清洗为纯数字。
     */
    private String getLandlineLocation(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (!digits.startsWith("0") || digits.length() < 4) {
            return "";
        }
        for (int len = 3; len <= 4; len++) {
            HscRegion region = ochRegionService.getOne(new LambdaQueryWrapper<HscRegion>()
                    .eq(HscRegion::getCityCode, digits.substring(0, len))
                    .eq(HscRegion::getLevel, "2")
                    .last("limit 1"));
            if (region != null) {
                return region.getMergerName();
            }
        }
        return "";
    }
}

