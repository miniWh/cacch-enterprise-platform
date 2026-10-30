package com.cacch.enterprise.integration.wecom.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业微信获取 access_token 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComAccessTokenResponse extends WeComBaseResponse {

    /**
     * 获取到的凭证
     */
    private String access_token;

    /**
     * 凭证有效期（秒），通常为 7200
     */
    private Integer expires_in;
}
