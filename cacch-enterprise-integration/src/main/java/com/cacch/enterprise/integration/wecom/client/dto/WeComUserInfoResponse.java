package com.cacch.enterprise.integration.wecom.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业微信根据 code 获取用户身份响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComUserInfoResponse extends WeComBaseResponse {

    /**
     * 企业微信用户 UserId（企业内部成员）
     */
    private String userid;

    /**
     * 外部联系人 openid（非企业成员时返回）
     */
    private String openid;

    /**
     * 外部联系人 id
     */
    private String external_userid;
}
