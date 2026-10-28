package com.cacch.enterprise.security;

import com.cacch.enterprise.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * REST 响应写出工具（认证/鉴权失败场景统一返回 Result JSON）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class RestResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RestResponseWriter() {
    }

    /**
     * 向响应写出统一返回体 JSON（HTTP 状态码恒为 200，业务语义由 code 表达）
     *
     * @param response HTTP 响应
     * @param code     业务返回码
     * @param message  提示消息
     */
    public static void write(HttpServletResponse response, int code, String message) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            OBJECT_MAPPER.writeValue(response.getWriter(), new Result<>(code, message, null));
        } catch (IOException e) {
            log.error("【Security】认证/鉴权失败响应写出异常, code={}", code, e);
        }
    }
}
