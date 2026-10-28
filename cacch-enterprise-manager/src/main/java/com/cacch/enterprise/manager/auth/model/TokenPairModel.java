package com.cacch.enterprise.manager.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 双令牌对（服务层内部模型，经 web 层 MapStruct 转换为 LoginTokenVO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPairModel implements Serializable {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 访问令牌有效期（秒），供前端计算静默刷新时机 */
    private long expiresInSeconds;
}
