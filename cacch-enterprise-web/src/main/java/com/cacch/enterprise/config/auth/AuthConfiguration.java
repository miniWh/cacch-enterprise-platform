package com.cacch.enterprise.config.auth;

import com.cacch.enterprise.common.config.CorsProperties;
import com.cacch.enterprise.common.config.auth.SecurityProperties;
import com.cacch.enterprise.common.config.auth.WeComProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 认证配置注册（SecurityProperties、WeComProperties 与 CorsProperties 构造器绑定）
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties({SecurityProperties.class, WeComProperties.class, CorsProperties.class})
public class AuthConfiguration {
}
