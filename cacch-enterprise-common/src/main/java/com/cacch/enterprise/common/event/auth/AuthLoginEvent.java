package com.cacch.enterprise.common.event.auth;

import com.cacch.enterprise.common.enums.auth.AuthChannelEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录行为事件（登录成功/失败）
 *
 * <p>登录编排层发布，async 模块监听器异步消费（本期以日志留痕，日志域立项后落库 sys_login_log）。
 * 事件内容不含明文密码。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginEvent implements Serializable {

    /**
     * 是否登录成功
     */
    private boolean success;

    /**
     * 登录渠道
     */
    private AuthChannelEnum channel;

    /**
     * 用户 ID（失败且账号不存在时为 null）
     */
    private Long userId;

    /**
     * 登录账号（工号或手机号原文，用于失败留痕）
     */
    private String account;

    /**
     * 登录来源 IP
     */
    private String loginIp;

    /**
     * 失败原因描述（成功时为 null），不包含敏感信息
     */
    private String failReason;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;
}
