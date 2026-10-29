package com.cacch.enterprise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson 全局配置：提供统一的 Jackson 2 ObjectMapper Bean
 *
 * <p>Spring Boot 4 主轨为 Jackson 3，不再自动装配 Jackson 2 的 ObjectMapper；
 * 本工程登录态与权限缓存的 Redis 序列化统一使用 Jackson 2，在此显式注册唯一 Bean，
 * 供 security 过滤器与 manager 模块构造器注入共用。
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 构建 Jackson 2 ObjectMapper（注册 JSR-310 日期时间模块）
     *
     * <p>LocalDateTime 按 ISO-8601 字符串序列化；反序列化忽略未知字段，兼容登录态结构演进。
     *
     * @return 全局共享的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
