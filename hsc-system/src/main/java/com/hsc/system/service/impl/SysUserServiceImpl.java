// 本文件基于 openCallHub（GPL-3.0）修改：修改者 jackzhang，2026-09
// Modified from openCallHub (GPL-3.0) by jackzhang, 2026-09.
package com.hsc.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageInfo;
import com.hsc.common.annotation.DataScope;
import com.hsc.common.base.BaseServiceImpl;
import com.hsc.common.config.redis.RedisService;
import com.hsc.common.constant.CacheConstants;
import com.hsc.common.enums.DeleteStatusEnum;
import com.hsc.common.enums.ExceptionStatusEnum;
import com.hsc.common.exception.CommonException;
import com.hsc.common.utils.SecurityUtils;
import com.hsc.system.domain.entity.SysUser;
import com.hsc.system.domain.entity.SysUserRole;
import com.hsc.system.domain.feature.IUserVo;
import com.hsc.system.domain.query.user.SysUserAddQuery;
import com.hsc.system.domain.query.user.SysUserQuery;
import com.hsc.system.domain.query.user.SysUserUpdateQuery;
import com.hsc.system.domain.vo.user.SysSimpleUserVo;
import com.hsc.system.domain.vo.user.SysUserVo;
import com.hsc.system.mapper.SysUserMapper;
import com.hsc.system.service.ISysRoleService;
import com.hsc.system.service.ISysUserDeptService;
import com.hsc.system.service.ISysUserRoleService;
import com.hsc.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户信息表(User)表服务实现类
 *
 * @author danmo
 * @since 2024-02-20 18:41:33
 */
@Slf4j
@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private ISysRoleService iSysRoleService;


    @Autowired
    private ISysUserRoleService iSysUserRoleService;

    @Autowired
    private ISysUserDeptService iSysUserDeptService;

    @Autowired
    private RedisService redisService;


    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, username).eq(SysUser::getDelFlag, 0));
    }


    @Override
    public SysUserVo getByUserId(Long userId) {
        return this.baseMapper.getByUserId(userId);
    }


    @Override
    @DataScope(deptAlias = "su")
    public PageInfo<SysUserVo> getPageList(SysUserQuery query) {
        startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
        List<SysUserVo> userList = new LinkedList<>();
        List<Long> userIds = this.baseMapper.selectUserIdsByQuery(query);
        if (!CollectionUtils.isEmpty(userIds)) {
            query.setUserIds(userIds);
            startPage(query.getPageIndex(), query.getPageSize(), query.getSortField(), query.getSort());
            List<SysUserVo> pageList = this.baseMapper.getPageList(query);
            userList.addAll(pageList);
        }
        decorate(userList);
        PageInfo<Long> pageTempInfo = new PageInfo<>(userIds);
        PageInfo<SysUserVo> pageInfo = new PageInfo<>(userList);
        pageInfo.setTotal(pageTempInfo.getTotal());
        pageInfo.setPageNum(pageTempInfo.getPageNum());
        pageInfo.setPageSize(pageTempInfo.getPageSize());
        return pageInfo;
    }

    @Override
    public List<SysUserVo> getList(SysUserQuery query) {
        return this.baseMapper.getList(query);
    }

    @Override
    public void decorate(IUserVo userVo) {
        if (Objects.isNull(userVo)) {
            return;
        }
        if (userVo.getCreateBy() == null && userVo.getUpdateBy() == null) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        userIds.add(userVo.getCreateBy());
        userIds.add(userVo.getUpdateBy());
        SysUserQuery query = new SysUserQuery();
        query.setUserIds(new ArrayList<>(userIds));

        List<SysUserVo> userList = getList(query);
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }
        Map<Long, String> userMap = userList.stream().collect(Collectors.toMap(SysUserVo::getUserId, SysUserVo::getNickName, (key1, key2) -> key1));
        userVo.setCreateName(userMap.get(userVo.getCreateBy()));
        userVo.setUpdateName(userMap.get(userVo.getUpdateBy()));
    }

    @Override
    public void decorate(List<? extends IUserVo> userVoList) {
        if (CollectionUtils.isEmpty(userVoList)) {
            return;
        }
        Set<Long> userIds = new HashSet<>();
        for (IUserVo userVo : userVoList) {
            if (userVo.getCreateBy() == null && userVo.getUpdateBy() == null) {
                continue;
            }
            userIds.add(userVo.getCreateBy());
            userIds.add(userVo.getUpdateBy());
        }
        List<Long> userIdList = userIds.stream().filter(Objects::nonNull).toList();
        SysUserQuery query = new SysUserQuery();
        query.setUserIds(new ArrayList<>(userIdList));
        List<SysUserVo> userList = getList(query);
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }
        Map<Long, String> userMap = userList.stream().collect(Collectors.toMap(SysUserVo::getUserId, SysUserVo::getNickName, (key1, key2) -> key1));
        for (IUserVo userVo : userVoList) {
            userVo.setCreateName(userMap.get(userVo.getCreateBy()));
            userVo.setUpdateName(userMap.get(userVo.getUpdateBy()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(SysUserAddQuery query) {
        //校验用户名称
        SysUserQuery userQuery = new SysUserQuery();
        userQuery.setUserName(query.getUserName());
        if (checkUserName(userQuery)) {
            throw new CommonException(ExceptionStatusEnum.ERROR_USER_NAME_EXISTENCE.getCode(), ExceptionStatusEnum.ERROR_USER_NAME_EXISTENCE.getMsg());
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(query, sysUser);
        // Query 的 getPassword 不再隐式加密（原 getter 加密导致 editPassWord 双重加密），此处显式加密
        sysUser.setPassword(SecurityUtils.encryptPassword(query.getPassword()));
        if (save(sysUser)) {
            if (!CollectionUtils.isEmpty(query.getRoleIds())) {
                List<SysUserRole> sysUserRoles = query.getRoleIds().stream().map(roleId -> {
                    SysUserRole sysUserRole = new SysUserRole();
                    sysUserRole.setUserId(sysUser.getUserId());
                    sysUserRole.setRoleId(roleId);
                    return sysUserRole;
                }).collect(Collectors.toList());
                iSysUserRoleService.saveBatch(sysUserRoles);
            }
            // 部门关联
            iSysUserDeptService.updateBatch(sysUser.getUserId(), query.getDeptIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(SysUserUpdateQuery query) {
        SysUser user = getById(query.getUserId());
        if(Objects.isNull(user)){
            throw new CommonException(ExceptionStatusEnum.ERROR_USERID_NOT_EXIST);
        }
        //校验用户名称
        SysUserQuery userQuery = new SysUserQuery();
        userQuery.setUserName(query.getUserName());
        if (!StringUtils.equals(query.getUserName(), user.getUserName()) && checkUserName(userQuery)) {
            throw new CommonException(ExceptionStatusEnum.ERROR_USER_NAME_EXISTENCE);
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(query, sysUser);
        sysUser.setUserId(query.getUserId());
        if (updateById(sysUser)) {
            iSysUserRoleService.updateBatch(query.getUserId(), query.getRoleIds());
            // 部门关联
            iSysUserDeptService.updateBatch(query.getUserId(), query.getDeptIds());
        }
    }


    @Override
    public void editPassWord(SysUserAddQuery query) {
        if (StringUtils.isEmpty(query.getPassword())) {
            throw new CommonException("密码不能为空");
        }
        // 此处单次加密入库；Query 的 getPassword 已去掉隐式加密（原 getter 加密会导致此处双重加密、密码必坏）
        update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, SecurityUtils.encryptPassword(query.getPassword()))
                .eq(SysUser::getUserId, query.getUserId()));
    }


    @Override
    public void editAvatar(Long userId, String avatar) {
        if (StringUtils.isEmpty(avatar)) {
            throw new CommonException("头像不能为空");
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getAvatar, avatar)
                .eq(SysUser::getUserId, userId));
    }


    @Override
    public void editProfile(Long userId, SysUserUpdateQuery query) {
        if (StringUtils.isEmpty(query.getNickName())) {
            throw new CommonException("昵称不能为空");
        }
        if (StringUtils.isNotEmpty(query.getPhone()) && !query.getPhone().matches("^1\\d{10}$")) {
            throw new CommonException("手机号格式不正确");
        }
        if (StringUtils.isNotEmpty(query.getEmail()) && !query.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new CommonException("邮箱格式不正确");
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getNickName, query.getNickName())
                .set(SysUser::getPhone, query.getPhone())
                .set(SysUser::getEmail, query.getEmail())
                .eq(SysUser::getUserId, userId));
    }


    @Override
    public void editPassword(Long userId, String oldPassword, String newPassword) {
        if (StringUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            throw new CommonException("新密码不能为空且至少6位");
        }
        SysUser sysUser = getById(userId);
        if (sysUser == null || !SecurityUtils.matchesPassword(oldPassword, sysUser.getPassword())) {
            throw new CommonException("旧密码不正确");
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, SecurityUtils.encryptPassword(newPassword))
                .eq(SysUser::getUserId, userId));
        // 改密后作废当前用户登录态（与 logout 同款删 key），强制重新登录
        redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + userId);
    }


    public static void main(String[] args) {
        String s = SecurityUtils.encryptPassword("12345678");
        System.out.println(s);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(SysUserQuery query) {
        if (!CollectionUtils.isEmpty(query.getUserIds())) {
            List<SysUser> userList = query.getUserIds().stream().map(userId -> {
                SysUser fsUser = new SysUser();
                fsUser.setDelFlag(1);
                fsUser.setUserId(userId);
                return fsUser;
            }).collect(Collectors.toList());

            boolean update = updateBatchById(userList);
            if (update) {
                iSysUserRoleService.update(new LambdaUpdateWrapper<SysUserRole>().set(SysUserRole::getDelFlag, DeleteStatusEnum.DELETE_YES.getIndex()).in(SysUserRole::getUserId, query.getUserIds()));
            }
        }
    }

    @Override
    public List<SysSimpleUserVo> getSelectList(SysUserQuery query) {
        return  this.baseMapper.getSelectList(query);
    }


    private boolean checkUserName(SysUserQuery query) {
        if (StringUtils.isNotEmpty(query.getUserName())) {
            SysUser sysUser = getByUsername(query.getUserName());
            if (Objects.nonNull(sysUser)) {
                return true;
            }
        }
        return false;
    }

}

