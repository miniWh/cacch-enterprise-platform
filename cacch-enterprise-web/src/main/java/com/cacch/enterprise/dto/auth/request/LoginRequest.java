package com.cacch.enterprise.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号密码登录请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class LoginRequest implements Serializable {

    /**
     * 登录账号：工号或 11 位手机号，由后端按格式自动识别
     */
    @NotBlank(message = "登录账号不能为空")
    private String account;

    /**
     * 登录密码明文，仅用于本次认证，禁止落日志
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 图形验证码唯一标识（获取验证码接口返回）
     */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    /**
     * 用户输入的图形验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
