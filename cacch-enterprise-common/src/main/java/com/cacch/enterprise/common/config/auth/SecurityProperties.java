package com.cacch.enterprise.common.config.auth;

import com.cacch.enterprise.common.enums.auth.LoginChannelPolicyEnum;
import lombok.Getter;

/**
 * 安全认证配置属性（cacch.security.*）
 *
 * <p>构造器绑定保证不可变；经 web 模块 AuthConfiguration 以 @EnableConfigurationProperties 注册。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class SecurityProperties {

    /** JWT 签名密钥（HS256），至少 32 字节，禁止硬编码到代码 */
    private final String jwtSecret;

    /** 访问令牌有效期（秒），默认 43200（12 小时） */
    private final Long accessTokenExpire;

    /** 刷新令牌有效期（秒），默认 43200（12 小时） */
    private final Long refreshTokenExpire;

    /** 多端登录策略：KICK_OUT 互踢（默认）/ COEXIST 共存（预留） */
    private final LoginChannelPolicyEnum loginChannelPolicy;

    /**
     * 构造配置属性（Spring Boot 构造器绑定）
     *
     * @param jwtSecret           JWT 签名密钥，至少 32 字节
     * @param accessTokenExpire   访问令牌有效期（秒），为 null 时取默认 43200
     * @param refreshTokenExpire  刷新令牌有效期（秒），为 null 时取默认 43200
     * @param loginChannelPolicy  多端登录策略，为 null 时取默认 KICK_OUT
     */
    public SecurityProperties(String jwtSecret, Long accessTokenExpire, Long refreshTokenExpire,
                              LoginChannelPolicyEnum loginChannelPolicy) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpire = accessTokenExpire == null ? 43200L : accessTokenExpire;
        this.refreshTokenExpire = refreshTokenExpire == null ? 43200L : refreshTokenExpire;
        this.loginChannelPolicy = loginChannelPolicy == null ? LoginChannelPolicyEnum.KICK_OUT : loginChannelPolicy;
    }
}
