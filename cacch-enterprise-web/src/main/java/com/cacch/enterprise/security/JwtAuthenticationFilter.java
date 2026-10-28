package com.cacch.enterprise.security;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.security.JwtTokenSupport;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * <p>请求头 Authorization: Bearer {accessToken} 解析访问令牌，校验 Redis 登录态存在且
 * jti 与当前会话一致（互踢/强制下线即时生效），通过后写入 LoginUserHolder 与 SecurityContext。
 * 令牌缺失时匿名放行（由授权规则决定 401）；令牌存在但无效时直接返回业务错误 JSON。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String PREFIX_BEARER = "Bearer ";

    private final JwtTokenSupport jwtTokenSupport;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 逐请求认证：解析令牌 → 校验登录态 → 写入上下文
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException 过滤器链异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER_AUTHORIZATION);
            if (header == null || !header.startsWith(PREFIX_BEARER)) {
                filterChain.doFilter(request, response);
                return;
            }
            LoginUser loginUser = resolveLoginUser(header.substring(PREFIX_BEARER.length()));
            LoginUserHolder.set(loginUser);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BizException e) {
            log.info("【Security】令牌认证未通过, code={}, message={}", e.getCode(), e.getMessage());
            RestResponseWriter.write(response, e.getCode(), e.getMessage());
        } finally {
            LoginUserHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 解析访问令牌并加载登录态
     *
     * @param token JWT 字符串
     * @return 登录用户上下文
     */
    private LoginUser resolveLoginUser(String token) {
        JwtTokenSupport.ParsedToken parsed = jwtTokenSupport.parse(token);
        if (!JwtTokenSupport.TYPE_ACCESS.equals(parsed.type())) {
            throw new BizException(ResultCode.AUTH_TOKEN_INVALID);
        }
        String json = stringRedisTemplate.opsForValue()
                .get(AuthCommonConstants.loginTokenRedisKey(parsed.userId()));
        LoginUser loginUser = readLoginUser(json);
        if (loginUser == null) {
            log.info("【Security】Redis 登录态不存在，判定会话已失效/被互踢, userId={}", parsed.userId());
            throw new BizException(ResultCode.AUTH_KICKED_OUT);
        }
        if (!parsed.jti().equals(loginUser.getJti())) {
            log.info("【Security】令牌 jti 与登录态不一致，判定旧会话被互踢, userId={}", parsed.userId());
            throw new BizException(ResultCode.AUTH_KICKED_OUT);
        }
        return loginUser;
    }

    /**
     * 反序列化登录态 JSON
     *
     * @param json 登录态 JSON，可为 null
     * @return 登录用户上下文；JSON 为空或解析失败时返回 null
     */
    private LoginUser readLoginUser(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (IOException e) {
            log.error("【Security】登录态 JSON 解析失败，视为未登录", e);
            return null;
        }
    }
}
