package com.cacch.enterprise.common.enums.auth;

/**
 * 多端登录策略枚举
 *
 * @author hongfu_zhou@cacch.com
 */
public enum LoginChannelPolicyEnum {

    /**
     * 互踢：同一账号重复登录时，新登录挤掉旧会话（当前默认策略）
     */
    KICK_OUT,

    /**
     * 共存：多端会话并存（预留，本期不实现）
     */
    COEXIST
}
