package com.cacch.enterprise.common.constant.auth;

/**
 * 认证业务域接口 PATH 常量
 *
 * <p>同时用于 Controller @RequestMapping 与 Spring Security 白名单配置。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class AuthPathConstants {

    private AuthPathConstants() {
    }

    /** 获取图形验证码 */
    public static final String PATH_CAPTCHA = "/api/v1/auth/captcha";

    /** 账号密码登录 */
    public static final String PATH_LOGIN = "/api/v1/auth/login";

    /** 静默刷新双令牌 */
    public static final String PATH_REFRESH = "/api/v1/auth/refresh";

    /** 登出 */
    public static final String PATH_LOGOUT = "/api/v1/auth/logout";

    /** 当前登录用户信息（含菜单树与权限标识集合） */
    public static final String PATH_PROFILE = "/api/v1/auth/profile";

    /** 修改密码（个人中心） */
    public static final String PATH_PROFILE_PASSWORD = "/api/v1/auth/profile/password";
}
