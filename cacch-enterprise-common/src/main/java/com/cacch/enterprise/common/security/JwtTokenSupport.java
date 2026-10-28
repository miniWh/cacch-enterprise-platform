package com.cacch.enterprise.common.security;

import com.cacch.enterprise.common.config.auth.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与解析工具（HS256）
 *
 * <p>放于 common 模块：manager 负责登录时签发、web 过滤器负责请求时解析，
 * 依据依赖方向规则（manager 不得依赖 web）将本组件置于公共模块。
 * access 与 refresh 使用同一密钥、以 type 声明区分。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Component
public class JwtTokenSupport {

    /** 令牌类型：访问令牌 */
    public static final String TYPE_ACCESS = "access";

    /** 令牌类型：刷新令牌 */
    public static final String TYPE_REFRESH = "refresh";

    private static final long CLOCK_SKEW_SECONDS = 60;

    private final SecretKey secretKey;

    private final SecurityProperties securityProperties;

    /**
     * 构造 JWT 工具
     *
     * @param securityProperties 安全配置（密钥与有效期）
     * @throws IllegalStateException 密钥长度不足 32 字节（HS256 要求）时抛出
     */
    public JwtTokenSupport(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        byte[] secretBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，请检查 cacch.security.jwt-secret 配置");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * 签发访问令牌
     *
     * @param userId   用户 ID（写入 sub）
     * @param username 工号（写入自定义声明 username）
     * @param jti      会话唯一标识，与 Redis 登录态中 jti 一致
     * @return 签名后的 access token 字符串
     */
    public String createAccessToken(Long userId, String username, String jti) {
        return createToken(userId, username, jti, TYPE_ACCESS,
                Duration.ofSeconds(securityProperties.getAccessTokenExpire()));
    }

    /**
     * 签发刷新令牌
     *
     * @param userId   用户 ID（写入 sub）
     * @param username 工号（写入自定义声明 username）
     * @param jti      刷新令牌唯一标识，与 Redis refresh Key 值一致
     * @return 签名后的 refresh token 字符串
     */
    public String createRefreshToken(Long userId, String username, String jti) {
        return createToken(userId, username, jti, TYPE_REFRESH,
                Duration.ofSeconds(securityProperties.getRefreshTokenExpire()));
    }

    /**
     * 解析并校验令牌签名与有效期
     *
     * @param token JWT 字符串
     * @return 解析结果（userId、username、type、jti）
     * @throws BizException 令牌为空、签名不合法或已过期时抛出（40104/40105）
     */
    public ParsedToken parse(String token) {
        if (token == null || token.isBlank()) {
            throw new com.cacch.enterprise.common.exception.BizException(
                    com.cacch.enterprise.common.result.ResultCode.AUTH_TOKEN_INVALID);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            return new ParsedToken(userId, claims.get("username", String.class),
                    claims.get("type", String.class), claims.getId());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new com.cacch.enterprise.common.exception.BizException(
                    com.cacch.enterprise.common.result.ResultCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new com.cacch.enterprise.common.exception.BizException(
                    com.cacch.enterprise.common.result.ResultCode.AUTH_TOKEN_INVALID);
        }
    }

    /**
     * 构建并签名令牌
     *
     * @param userId   用户 ID
     * @param username 工号
     * @param jti      令牌唯一标识
     * @param type     令牌类型（access/refresh）
     * @param ttl      有效期
     * @return 签名后的令牌字符串
     */
    private String createToken(Long userId, String username, String jti, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", type)
                .id(jti)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成会话/令牌唯一标识（jti）
     *
     * @return 32 位无连字符 UUID
     */
    public static String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 令牌解析结果
     *
     * @param userId   用户 ID
     * @param username 工号
     * @param type     令牌类型（access/refresh）
     * @param jti      令牌唯一标识
     */
    public record ParsedToken(Long userId, String username, String type, String jti) {
    }
}
