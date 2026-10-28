package com.cacch.enterprise.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 静默刷新双令牌请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class RefreshTokenRequest implements Serializable {

    /** 刷新令牌（登录/上次刷新时签发） */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
