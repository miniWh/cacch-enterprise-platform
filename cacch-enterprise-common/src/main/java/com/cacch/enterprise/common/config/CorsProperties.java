package com.cacch.enterprise.common.config;

import java.util.List;

/**
 * 跨域配置属性（cacch.cors.*）
 *
 * <p>跨业务域基础设施配置，构造器绑定保证不可变；经 web 模块 AuthConfiguration 注册。
 *
 * @param allowedOrigins 允许的跨域来源列表（前端开发/部署地址），为空时全部拒绝
 * @author hongfu_zhou@cacch.com
 */
public record CorsProperties(List<String> allowedOrigins) {

    /**
     * 构造配置属性（Spring Boot 构造器绑定）
     *
     * @param allowedOrigins 允许的跨域来源列表，为 null 时按空列表处理
     */
    public CorsProperties(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
