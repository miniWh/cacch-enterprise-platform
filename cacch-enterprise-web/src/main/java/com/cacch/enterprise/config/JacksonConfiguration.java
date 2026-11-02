package com.cacch.enterprise.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 全局配置
 *
 * <p>提供两类定制：
 * <ul>
 * <li>Jackson 2 {@link ObjectMapper}：Spring Boot 4 主轨为 Jackson 3，不再自动装配 Jackson 2，
 * 本工程登录态与权限缓存的 Redis 序列化统一使用 Jackson 2，在此显式注册唯一 Bean，供
 * security 过滤器与 manager 模块构造器注入共用；</li>
 * <li>Jackson 3 {@link JsonMapperBuilderCustomizer}：定制 MVC HTTP 消息序列化，
 * Long 统一输出为字符串，规避雪花 ID 超出 JS 安全整数上限的精度失真问题。</li>
 * </ul>
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

    /**
     * 定制 Jackson 3 MVC 序列化：Long 类型统一输出为 JSON 字符串
     *
     * <p>业务主键由雪花算法生成（19 位 BIGINT），超出 JS Number 安全整数上限（2^53），
     * 以 JSON 数值输出会被浏览器静默失真，导致前端按 ID 回传的查询、编辑、授权等操作
     * 命中不到记录（表现为 40400 资源不存在）。序列化为字符串后前端零精度损失；
     * 请求侧 Jackson 自动将数字或字符串反序列化为 Long，接口调用方无需任何适配。
     *
     * @return Jackson 3 JsonMapper 构建定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.addModule(module);
    }
}
