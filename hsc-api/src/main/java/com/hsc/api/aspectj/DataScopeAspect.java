package com.hsc.api.aspectj;

import com.hsc.common.annotation.DataScope;
import com.hsc.security.authority.LoginUserInfo;
import com.hsc.security.utils.SecurityUtils;
import com.hsc.system.domain.entity.SysDept;
import com.hsc.system.service.ISysDeptService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门数据权限切面(方案C)。
 *
 * <p>{@code @Around} 拦截带 {@link DataScope} 的 Service 方法，按当前用户 dataScope 计算可见部门 deptIds，
 * 反射调用 query 参数的 {@code setDeptIds(List)} 注入，由 mapper XML 用参数化 foreach 完成 {@code dept_id IN (...)} 过滤。
 *
 * <p>规则：
 * <ul>
 *   <li>未登录线程(定时任务/异步)或超管 → 不追加过滤</li>
 *   <li>dataScope 含 1(全部) → 不追加过滤</li>
 *   <li>含 2(本部门及以下) → 自己 + 所有后代(ISysDeptService.getDescendants 不含自身，手动加)</li>
 *   <li>含 3(本部门) → 仅自己</li>
 *   <li>含 4(本人) → 本轮占位，降级为本部门(后续按 create_by/user_id 精细化)</li>
 *   <li>有 dataScope 但 deptId 为 null(配置异常) → 强制空集(-1)防泄漏并告警</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Autowired
    private ISysDeptService iSysDeptService;

    @Around("@annotation(dataScope)")
    public Object doAround(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        LoginUserInfo user = SecurityUtils.getCurrentUserInfo();
        // 未登录(定时任务/异步线程)或超管 → 不过滤
        if (user == null || SecurityUtils.isAdmin()) {
            return pjp.proceed();
        }
        List<Integer> dataScopeList = user.getDataScope();
        if (dataScopeList == null || dataScopeList.isEmpty()) {
            return pjp.proceed();
        }

        List<Long> deptIds = resolveDeptIds(user, dataScopeList);
        if (deptIds == null) {
            // "全部数据" → 不过滤
            return pjp.proceed();
        }
        if (deptIds.isEmpty()) {
            // 有数据范围但未挂部门(配置异常) → 安全降级为空集，防泄漏
            log.warn("用户[userId={}]配置了数据范围{}但无 deptId，本次查询强制返回空，请检查用户部门归属",
                    user.getUserId(), dataScopeList);
            deptIds.add(-1L);
        }

        // 把 deptIds 塞进每个支持 setDeptIds(List) 的参数
        for (Object arg : pjp.getArgs()) {
            if (arg == null) {
                continue;
            }
            try {
                Method setter = arg.getClass().getMethod("setDeptIds", List.class);
                setter.invoke(arg, deptIds);
            } catch (NoSuchMethodException ignore) {
                // 该参数不支持数据权限，跳过
            }
        }
        return pjp.proceed();
    }

    /**
     * 计算当前用户可见的部门ID集合。
     *
     * @return null 表示"全部数据"(不过滤)；空列表表示有范围但算不出具体部门
     */
    private List<Long> resolveDeptIds(LoginUserInfo user, List<Integer> dataScopeList) {
        if (dataScopeList.contains(1)) {
            return null;
        }
        Long deptId = user.getDeptId();
        List<Long> deptIds = new ArrayList<>();
        if (dataScopeList.contains(2)) {
            // 本部门及以下：自己 + 所有后代
            if (deptId != null) {
                deptIds.add(deptId);
                List<SysDept> descendants = iSysDeptService.getDescendants(deptId);
                if (descendants != null) {
                    for (SysDept d : descendants) {
                        deptIds.add(d.getDeptId());
                    }
                }
            }
        } else if (dataScopeList.contains(3)) {
            // 本部门
            if (deptId != null) {
                deptIds.add(deptId);
            }
        } else if (dataScopeList.contains(4)) {
            // 本人：本轮占位，降级为本部门(后续按 create_by/user_id 精细化)
            if (deptId != null) {
                deptIds.add(deptId);
            }
            log.debug("dataScope=4(本人)本轮降级为本部门，userId={}, deptId={}", user.getUserId(), deptId);
        }
        return deptIds;
    }
}
