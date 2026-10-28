package com.cacch.enterprise.security;

import com.cacch.enterprise.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * REST 未登录入口点（替代默认 302/401 页面，返回统一业务码 JSON）
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 未认证请求统一返回 40100 业务码
     *
     * @param request       HTTP 请求
     * @param response      HTTP 响应
     * @param authException 认证异常
     * @throws IOException 响应写出异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        RestResponseWriter.write(response, ResultCode.AUTH_NOT_LOGIN.getCode(),
                ResultCode.AUTH_NOT_LOGIN.getMessage());
    }
}
