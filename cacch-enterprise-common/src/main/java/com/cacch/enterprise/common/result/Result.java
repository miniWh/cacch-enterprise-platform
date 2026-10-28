package com.cacch.enterprise.common.result;

import com.cacch.enterprise.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体
 *
 * <p>所有 REST 接口统一以该结构包装返回值；业务码语义见 {@link ResultCode}。
 *
 * @param <T> 业务数据类型
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 业务返回码，0 表示成功，其余见 ResultCode
     */
    private int code;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 业务数据，无数据时为 null
     */
    private T data;

    /**
     * 构建成功返回体（无数据）
     *
     * @param <T> 业务数据类型
     * @return code=0 的成功返回体，data 为 null
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 构建成功返回体
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return code=0 的成功返回体
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构建失败返回体
     *
     * @param resultCode 业务返回码枚举
     * @param <T>        业务数据类型
     * @return 携带错误码与默认提示的失败返回体
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 构建失败返回体（自定义提示消息）
     *
     * @param resultCode 业务返回码枚举
     * @param message    自定义提示消息，覆盖枚举默认消息
     * @param <T>        业务数据类型
     * @return 携带错误码与自定义提示的失败返回体
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * 由业务异常构建失败返回体
     *
     * @param e   业务异常，携带返回码与提示消息
     * @param <T> 业务数据类型
     * @return 与异常信息一致的失败返回体
     */
    public static <T> Result<T> fail(BizException e) {
        return new Result<>(e.getCode(), e.getMessage(), null);
    }
}
