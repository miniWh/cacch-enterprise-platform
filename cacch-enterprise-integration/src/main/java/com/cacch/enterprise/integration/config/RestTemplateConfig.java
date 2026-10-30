package com.cacch.enterprise.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方 HTTP 客户端配置
 *
 * <p>为 integration 模块提供统一的 RestTemplate Bean，设置连接与读取超时，
 * 避免外部系统响应慢导致线程长时间阻塞。
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 构造第三方调用专用 RestTemplate
     *
     * @return 配置了超时的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
