// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.MD5Utils;
import com.hsc.common.utils.StringUtils;
import com.hsc.system.domain.entity.KoSubscriber;
import com.hsc.system.domain.query.subsriber.KoSubscriberAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberBatchAddQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberQuery;
import com.hsc.system.domain.query.subsriber.KoSubscriberUpdateQuery;
import com.hsc.system.domain.vo.sip.KoSubscriberVo;
import com.hsc.system.domain.vo.sip.SipSimpleVo;
import com.hsc.system.mapper.KoSubscriberMapper;
import com.hsc.system.service.IKoSubscriberService;
import com.hsc.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * (Subscriber)
 *
 * @author danmo
 * @date 2024-07-29 10:49:24
 */
@Service
public class KoSubscriberServiceImpl extends BaseServiceImpl<KoSubscriberMapper, KoSubscriber> implements IKoSubscriberService {

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public void add(KoSubscriberAddQuery query) {
        Boolean checked = checkUserName(query.getUsername());
        if (checked) {
            throw new CommonException("账号已存在");
        }
        Integer terminalType = query.getTerminalType() == null ? 0 : query.getTerminalType();
        String password = resolvePassword(terminalType, query.getPassword());
        KoSubscriber subscriber = new KoSubscriber();
        subscriber.setUsername(query.getUsername());
        subscriber.setPassword(password);
        subscriber.setStatus(query.getStatus());
        subscriber.setTerminalType(terminalType);
        subscriber.setDomain(query.getDomain());
        if(StringUtils.isNotBlank(query.getDomain()) && StringUtils.isBlank(query.getHa1())){
            String ha1 = MD5Utils.MD5(query.getUsername() + ":" + query.getDomain() + ":" + password);
            query.setHa1(ha1);
        }
        if(StringUtils.isNotBlank(query.getDomain()) && StringUtils.isBlank(query.getHa1b())){
            String ha1b = MD5Utils.MD5(query.getUsername() + ":" + query.getDomain() + ":" + password);
            query.setHa1b(ha1b);
        }
        subscriber.setHa1(query.getHa1());
        subscriber.setHa1b(query.getHa1b());
        subscriber.setVmpin(query.getVmpin());
        save(subscriber);
    }

    /**
     * 按终端类型解析密码:软电话(0)后端生成强密码(用户无感,忽略前端值);
     * 座机(1)用前端值,校验纯数字且≥8位(话机键盘只有数字)。
     */
    private String resolvePassword(Integer terminalType, String input) {
        if (terminalType == null || terminalType == 0) {
            return RandomUtil.randomString(16);
        }
        if (StringUtils.isBlank(input) || input.length() < 8 || !input.matches("\\d+")) {
            throw new CommonException("座机密码必须为纯数字且不少于8位");
        }
        return input;
    }

    private Boolean checkUserName(String username) {
        KoSubscriber subscriber = getByUserName(username);
        return Objects.nonNull(subscriber);
    }

    @Override
    public void batchAdd(KoSubscriberBatchAddQuery query) {
        int initNum = query.getInitNum();

        String prefix = StringUtils.rightPad("", 4, "0");

        List<KoSubscriber> subscriberList = new LinkedList<>();
        for (int i = 0; i < query.getNumber(); i++) {
            KoSubscriber subscriber = new KoSubscriber();
            subscriber.setUsername(prefix + initNum);
            subscriber.setTerminalType(0);
            subscriber.setPassword(RandomUtil.randomString(16));
            initNum++;
            subscriberList.add(subscriber);
        }
        if (CollectionUtil.isNotEmpty(subscriberList)) {
            List<String> userNameList = subscriberList.stream().map(KoSubscriber::getUsername).collect(Collectors.toList());
            List<KoSubscriber> nameList = getByUserNameList(userNameList);
            if (CollectionUtil.isNotEmpty(nameList)) {
                throw new CommonException("账号已存在");
            }
        }
        saveBatch(subscriberList);
    }

    @Override
    public void edit(KoSubscriberUpdateQuery query) {
        KoSubscriber subscriber = getById(query.getId());
        if (Objects.isNull(subscriber)){
            throw new CommonException("无效ID");
        }
        if(!StringUtils.equals(subscriber.getUsername(), query.getUsername()) && checkUserName(query.getUsername())){
            throw new CommonException("账号已存在");
        }else if (StringUtils.isNotBlank(query.getUsername())) {
            subscriber.setUsername(query.getUsername());
        }

        // 先定 password(改则校验+设),ha1/ha1b 才能用最终 password 算(否则改密码时 ha1 用旧密码不一致)
        if (StringUtils.isNotBlank(query.getPassword())) {
            // 改密码:统一按座机规则校验纯数字≥8(软电话前端密码隐藏,不会传)
            if (query.getPassword().length() < 8 || !query.getPassword().matches("\\d+")) {
                throw new CommonException("密码必须为纯数字且不少于8位");
            }
            subscriber.setPassword(query.getPassword());
        }
        if(StringUtils.isNotBlank(query.getDomain()) && StringUtils.isBlank(query.getHa1())){
            String ha1 = MD5Utils.MD5(query.getUsername() + ":" + query.getDomain() + ":" + subscriber.getPassword());
            query.setHa1(ha1);
        }
        if(StringUtils.isNotBlank(query.getDomain()) && StringUtils.isBlank(query.getHa1b())){
            String ha1b = MD5Utils.MD5(query.getUsername() + ":" + query.getDomain() + ":" + subscriber.getPassword());
            query.setHa1b(ha1b);
        }
        if (StringUtils.isNotBlank(query.getDomain())) {
            subscriber.setDomain(query.getDomain());
        }
        if (StringUtils.isNotBlank(query.getHa1())) {
            subscriber.setHa1(query.getHa1());
        }
        if (StringUtils.isNotBlank(query.getHa1b())) {
            subscriber.setHa1b(query.getHa1b());
        }
        if (StringUtils.isNotBlank(query.getVmpin())) {
            subscriber.setVmpin(query.getVmpin());
        }
        if (Objects.nonNull(query.getStatus())) {
            subscriber.setStatus(query.getStatus());
        }
        if (Objects.nonNull(query.getTerminalType())) {
            subscriber.setTerminalType(query.getTerminalType());
        }
        updateById(subscriber);
    }

    @Override
    public KoSubscriber getDetail(Integer id) {
        return getById(id);
    }

    @Override
    public void delete(KoSubscriberQuery query) {
        remove(new LambdaQueryWrapper<KoSubscriber>().eq(KoSubscriber::getId, query.getId()));
    }

    @Override
    public List<KoSubscriberVo> getList(KoSubscriberQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public KoSubscriber getByUserName(String username) {
        return this.baseMapper.getByUserName(username);
    }

    @Override
    public List<KoSubscriberVo> getPageList(KoSubscriberQuery query) {
        startPage(query.getPageIndex(), query.getPageSize());
        List<KoSubscriberVo> list = getList(query);
        if (CollectionUtil.isNotEmpty(list)){
            sysUserService.decorate(list);
        }
        return list;
    }

    @Override
    public List<SipSimpleVo> selectList() {
        return selectList(null);
    }

    @Override
    public List<SipSimpleVo> selectList(Integer terminalType) {
        List<KoSubscriber> list = list(new LambdaQueryWrapper<KoSubscriber>()
                .eq(KoSubscriber::getStatus, 0)
                .eq(terminalType != null, KoSubscriber::getTerminalType, terminalType));
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.parallelStream().map(koSubscriber -> {
            SipSimpleVo sipSimpleVo = new SipSimpleVo();
            sipSimpleVo.setSipId(koSubscriber.getId());
            sipSimpleVo.setSipName(koSubscriber.getUsername());
            sipSimpleVo.setTerminalType(koSubscriber.getTerminalType());
            return sipSimpleVo;
        }).toList();
    }

    public List<KoSubscriber> getByUserNameList(List<String> userNameList) {
        return this.baseMapper.getByUserNameList(userNameList);
    }
}
