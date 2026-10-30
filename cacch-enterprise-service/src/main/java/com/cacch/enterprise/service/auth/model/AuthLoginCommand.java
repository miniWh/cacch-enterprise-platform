package com.cacch.enterprise.service.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录认证命令（服务层内部模型，经 web 层 MapStruct 转换自请求 DTO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginCommand implements Serializable {

    /**
     * 登录账号（工号或手机号，由实现方按格式自动识别）
     */
    private String account;

    /**
     * 明文密码，仅用于本次认证，禁止落日志
     */
    private String password;

    /**
     * 登录来源 IP
     */
    private String loginIp;

    /**
     * 企微授权码（仅 WECOM_INAPP 渠道使用），由企微工作台打开应用主页 URL 时携带，一次性有效
     */
    private String code;
}
