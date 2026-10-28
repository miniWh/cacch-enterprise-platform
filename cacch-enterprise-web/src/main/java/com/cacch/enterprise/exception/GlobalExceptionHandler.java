package com.cacch.enterprise.exception;

import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器（异常统一转换为 Result JSON）
 *
 * <p>业务异常按 WARN 记录；系统异常按 ERROR 记录堆栈；参数校验异常返回 40002。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     *
     * @param e 业务异常
     * @return 与异常信息一致的失败返回体
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("【Global】业务异常, code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e);
    }

    /**
     * 请求体参数校验异常处理（@Valid 校验失败）
     *
     * @param e 校验异常
     * @return 40002 失败返回体（消息为首个字段错误提示）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ResultCode.PARAM_INVALID.getMessage());
        log.warn("【Global】参数校验失败, message={}", message);
        return Result.fail(ResultCode.PARAM_INVALID, message);
    }

    /**
     * 方法级参数校验异常处理（路径变量/查询参数校验失败）
     *
     * @param e 校验异常
     * @return 40002 失败返回体
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.warn("【Global】方法参数校验失败, message={}", e.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID);
    }

    /**
     * 请求体不可读异常处理（JSON 格式错误等）
     *
     * @param e 请求体解析异常
     * @return 40002 失败返回体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("【Global】请求体解析失败, message={}", e.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID, "请求体格式不正确");
    }

    /**
     * 静态资源不存在异常处理（未知路径）
     *
     * @param e 资源不存在异常
     * @return 40400 失败返回体
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("【Global】请求路径不存在, path={}", e.getResourcePath());
        return Result.fail(ResultCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 未捕获系统异常处理
     *
     * @param e 系统异常
     * @return 99900 失败返回体
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("【Global】系统异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
