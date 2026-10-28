package com.cacch.enterprise.common.constant.auth;

import com.cacch.enterprise.common.constant.redis.RedisConstants;

/**
 * 认证业务域公共常量
 *
 * <p>承载登录认证跨接口共享的请求参数名、正则、限流阈值与 Redis Key 组装方法。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class AuthCommonConstants {

    private AuthCommonConstants() {
    }

    /** 手机号格式（登录时按输入格式自动识别账号类型） */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /** 密码复杂度：长度 8~64 位，须同时包含字母与数字 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    /** 图形验证码有效期（秒），一次性使用，校验后（无论对错）即失效 */
    public static final int CAPTCHA_EXPIRE_SECONDS = 300;

    /** 登录接口 IP 限流：窗口期内最大请求数 */
    public static final int LOGIN_RATE_LIMIT_COUNT = 10;

    /** 验证码接口 IP 限流：窗口期内最大请求数 */
    public static final int CAPTCHA_RATE_LIMIT_COUNT = 30;

    /** IP 限流窗口长度（秒） */
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    /** 登录渠道策略配置默认值：互踢（新登录挤掉旧会话） */
    public static final String DEFAULT_LOGIN_CHANNEL_POLICY = "KICK_OUT";

    /**
     * 组装图形验证码缓存 Key（一次性，校验后即删）
     *
     * @param captchaId 验证码唯一标识
     * @return 形如 enterprise:auth:captcha:{captchaId} 的 Key
     */
    public static String captchaRedisKey(String captchaId) {
        return RedisConstants.KEY_PREFIX + "auth:captcha:" + captchaId;
    }

    /**
     * 组装登录态缓存 Key（值为 LoginUser JSON，含当前会话 access jti）
     *
     * @param userId 用户 ID
     * @return 形如 enterprise:auth:token:{userId} 的 Key
     */
    public static String loginTokenRedisKey(Long userId) {
        return RedisConstants.KEY_PREFIX + "auth:token:" + userId;
    }

    /**
     * 登录态 Key 的 SCAN 通配模式（在线用户列表遍历用）
     *
     * @return 形如 enterprise:auth:token:* 的匹配模式
     */
    public static String loginTokenScanPattern() {
        return RedisConstants.KEY_PREFIX + "auth:token:*";
    }

    /**
     * 组装刷新凭证缓存 Key（值为当前有效 refresh jti，轮换更新）
     *
     * @param userId 用户 ID
     * @return 形如 enterprise:auth:refresh:{userId} 的 Key
     */
    public static String refreshTokenRedisKey(Long userId) {
        return RedisConstants.KEY_PREFIX + "auth:refresh:" + userId;
    }

    /**
     * 组装登录接口 IP 限流计数 Key
     *
     * @param ip 客户端 IP
     * @return 形如 enterprise:auth:ratelimit:login:{ip} 的 Key
     */
    public static String loginRateLimitRedisKey(String ip) {
        return RedisConstants.KEY_PREFIX + "auth:ratelimit:login:" + ip;
    }

    /**
     * 组装验证码接口 IP 限流计数 Key
     *
     * @param ip 客户端 IP
     * @return 形如 enterprise:auth:ratelimit:captcha:{ip} 的 Key
     */
    public static String captchaRateLimitRedisKey(String ip) {
        return RedisConstants.KEY_PREFIX + "auth:ratelimit:captcha:" + ip;
    }
}
