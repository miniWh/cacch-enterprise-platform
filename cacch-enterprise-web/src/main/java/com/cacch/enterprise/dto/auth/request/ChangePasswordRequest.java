package com.cacch.enterprise.dto.auth.request;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人中心修改密码请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class ChangePasswordRequest implements Serializable {

    /**
     * 原密码明文
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码明文：8~64 位，须同时包含字母与数字
     */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = AuthCommonConstants.PASSWORD_REGEX, message = "新密码须为 8~64 位且同时包含字母与数字")
    private String newPassword;
}
