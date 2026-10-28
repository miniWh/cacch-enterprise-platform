package com.cacch.enterprise.common.enums.auth;

/**
 * 登录渠道枚举
 *
 * <p>本期仅实现 PASSWORD（账号密码）；企微扫码/自建应用免登为预留渠道，
 * 通过 {@code IAuthChannelService} 渠道策略接口扩展，核心登录编排不变。
 *
 * @author hongfu_zhou@cacch.com
 */
public enum AuthChannelEnum {

    /** 账号密码登录（工号或手机号 + 密码） */
    PASSWORD,

    /** 企业微信扫码登录（预留） */
    WECOM_QR,

    /** 企业微信自建应用免登（预留） */
    WECOM_INAPP
}
