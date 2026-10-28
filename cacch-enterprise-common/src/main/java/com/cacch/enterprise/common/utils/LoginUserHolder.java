package com.cacch.enterprise.common.utils;

import com.cacch.enterprise.common.model.auth.LoginUser;

/**
 * 登录用户上下文持有器（ThreadLocal）
 *
 * <p>web 层 JwtAuthenticationFilter 在请求开始时写入、finally 中清理；
 * 数据权限拦截器与业务层读取当前登录用户。异步线程（@Async/定时任务）中不可用。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class LoginUserHolder {

    private LoginUserHolder() {
    }

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    /**
     * 写入当前请求的登录用户上下文
     *
     * @param loginUser 登录用户上下文，由认证过滤器构建
     */
    public static void set(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    /**
     * 读取当前登录用户上下文
     *
     * @return 当前登录用户；匿名请求或异步线程中返回 null
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 读取当前登录用户 ID
     *
     * @return 用户 ID；无上下文时返回 null
     */
    public static Long getUserId() {
        LoginUser user = CONTEXT.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 清理当前线程上下文，防止线程池复用导致的数据串号
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
