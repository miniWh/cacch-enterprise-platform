package com.cacch.enterprise.common.exception;

import com.cacch.enterprise.common.result.ResultCode;
import lombok.Getter;

/**
 * 统一业务异常
 *
 * <p>Service/Manager 层通过 {@code throw new BizException(ResultCode.XXX)} 抛出，
 * 由 web 模块 GlobalExceptionHandler 统一转换为 {@link com.cacch.enterprise.common.result.Result} 返回。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 业务返回码
     */
    private final int code;

    /**
     * 以返回码默认消息构造业务异常
     *
     * @param resultCode 业务返回码枚举
     */
    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 以自定义消息构造业务异常（覆盖返回码默认消息）
     *
     * @param resultCode 业务返回码枚举
     * @param message    自定义提示消息
     */
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
