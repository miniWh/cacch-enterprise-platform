package com.cacch.enterprise.dto.system.request;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置用户密码请求（管理员操作，目标用户会话将被清除强制重新登录）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class ResetPasswordRequest implements Serializable {

    /**
     * 新密码明文：8~64 位，须同时包含字母与数字
     */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = AuthCommonConstants.PASSWORD_REGEX, message = "新密码须为 8~64 位且同时包含字母与数字")
    private String newPassword;
}
