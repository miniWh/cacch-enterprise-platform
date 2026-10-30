package com.cacch.enterprise.common.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业微信自建应用配置属性（cacch.wecom.*）
 *
 * <p>构造器绑定保证不可变；经 web 模块 AuthConfiguration 以 @EnableConfigurationProperties 注册。
 * 凭证类字段（secret）禁止硬编码，须由外置配置或环境变量注入。
 *
 * @param corpid  企业 ID（企业微信管理后台「我的企业」中获取）
 * @param agentId 自建应用 AgentId
 * @param secret  自建应用 Secret，禁止写入日志
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "cacch.wecom")
public record WeComProperties(String corpid, Integer agentId, String secret) {

    /**
     * 构造企微配置属性（Spring Boot 构造器绑定）
     *
     * @param corpid  企业 ID，为 null 时不启用企微能力
     * @param agentId 自建应用 AgentId，为 null 时不启用企微能力
     * @param secret  自建应用 Secret
     */
    public WeComProperties {
    }
}
