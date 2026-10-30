package com.cacch.enterprise.integration.wecom.client.dto;

import lombok.Data;

/**
 * 企业微信 API 响应公共基类
 *
 * <p>企微所有接口响应均包含 errcode（错误码）与 errmsg（错误信息），
 * 各业务响应类继承本类后仅需声明业务特有字段。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComBaseResponse {

    /**
     * 错误码，0 表示成功
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 判断企微接口调用是否成功（errcode 为 0）
     *
     * @return errcode 为 null 或非 0 时返回 false
     */
    public boolean isSuccess() {
        return errcode == null || errcode != 0;
    }
}
