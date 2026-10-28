package com.cacch.enterprise.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按钮级权限校验注解
 *
 * <p>标记在 Controller 方法上，切面在请求进入前校验当前登录用户是否持有对应权限标识；
 * 权限标识与 sys_menu 表按钮节点 perms 字段及前端 v-permission 指令一一对应。
 * <p>示例：{@code @RequiresPermission(SystemPermissionConstants.USER_ADD)}
 *
 * @author hongfu_zhou@cacch.com
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPermission {

    /**
     * 所需权限标识
     *
     * @return 形如 system:user:add 的权限标识常量，编译期引用常量类防止魔法字符串
     */
    String value();
}
