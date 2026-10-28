package com.cacch.enterprise.dto.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 双令牌视图对象
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginTokenVO implements Serializable {

    /** 访问令牌，后续请求放入 Authorization: Bearer {accessToken} */
    private String accessToken;

    /** 刷新令牌，用于静默续期 */
    private String refreshToken;

    /** 访问令牌有效期（秒），前端据此计算静默刷新时机 */
    private Long expiresInSeconds;

    /** 令牌类型，固定 Bearer */
    private String tokenType;
}
