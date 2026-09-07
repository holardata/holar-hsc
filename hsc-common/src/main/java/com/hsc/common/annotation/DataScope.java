package com.hsc.common.annotation;

import java.lang.annotation.*;

/**
 * 部门数据权限：标注在 Service 查询方法上，由 DataScopeAspect 把当前用户可见的部门ID集合(deptIds)
 * 塞进 query 参数，mapper XML 用参数化 foreach 过滤。
 *
 * <p>方案要点：切面只算 deptIds 并写入 query，不拼字符串 SQL、不在 XML 用占位符，
 * 避免 SQL 注入与"忘贴占位符就泄漏"的隐患。
 *
 * <p>数据范围(dataScope)：1 全部 / 2 本部门及以下 / 3 本部门 / 4 本人。多角色取最宽松(并集)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门字段在 SQL 中的表别名(如 sys_user 别名 "su")，供 mapper XML 拼接 {@code 别名.dept_id} 时参照
     */
    public String deptAlias() default "";

    /**
     * 本人数据权限(dataScope=4)对应字段别名，预留(本轮占位)
     */
    public String userAlias() default "";
}
