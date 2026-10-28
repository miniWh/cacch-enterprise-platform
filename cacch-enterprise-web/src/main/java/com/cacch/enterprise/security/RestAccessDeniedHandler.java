package com.cacch.enterprise.security;

import com.cacch.enterprise.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * REST 无权限处理器（替代默认 403 页面，返回统一业务码 JSON）
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 已认证但无权限的请求统一返回 40300 业务码
     *
     * @param request               HTTP 请求
     * @param response              HTTP 响应
     * @param accessDeniedException 拒绝访问异常
     */
    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) {
        RestResponseWriter.write(response, ResultCode.AUTH_PERMISSION_DENIED.getCode(),
                ResultCode.AUTH_PERMISSION_DENIED.getMessage());
    }
}
