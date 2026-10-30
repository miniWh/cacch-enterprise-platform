package com.cacch.enterprise.common.constant.auth;

import com.cacch.enterprise.common.constant.redis.RedisConstants;

/**
 * 企业微信认证相关常量（自建应用免登渠道）
 *
 * <p>承载企微 API 地址、access_token 缓存 Key 与有效期等跨接口共享常量。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class WeComAuthConstants {

    private WeComAuthConstants() {
    }

    /**
     * 企微获取 access_token 接口地址
     */
    public static final String URL_GET_TOKEN = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpid}&corpsecret={secret}";

    /**
     * 企微根据 code 获取用户身份接口地址
     */
    public static final String URL_GET_USER_INFO = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token={accessToken}&code={code}";

    /**
     * 企微 access_token 有效期（秒），企微官方为 7200s，缓存略短避免边界失效
     */
    public static final int ACCESS_TOKEN_CACHE_SECONDS = 7000;

    /**
     * 组装企微 access_token 缓存 Key
     *
     * @param corpid 企业 ID
     * @return 形如 enterprise:auth:wecom:access_token:{corpid} 的 Key
     */
    public static String accessTokenRedisKey(String corpid) {
        return RedisConstants.KEY_PREFIX + "auth:wecom:access_token:" + corpid;
    }
}
