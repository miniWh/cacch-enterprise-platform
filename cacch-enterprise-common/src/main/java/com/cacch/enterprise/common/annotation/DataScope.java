package com.cacch.enterprise.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限标记注解
 *
 * <p>标记在 Mapper 方法上，数据权限拦截器据此向目标表追加部门过滤条件；
 * 未标记的方法不受数据权限影响（如登录查询等旁路 SQL）。
 *
 * @author hongfu_zhou@cacch.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataScope {

    /**
     * 目标表别名
     *
     * @return SQL 中用户表（含 dept_id 列）的别名；无别名时为空字符串
     */
    String tableAlias() default "";
}
