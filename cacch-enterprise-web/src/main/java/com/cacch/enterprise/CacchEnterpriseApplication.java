package com.cacch.enterprise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 企业管理平台启动类
 *
 * <p>本地开发：main 方法启动（内嵌容器，无需安装 Tomcat）；生产：War 包部署外部 Tomcat 11.0.x
 * （Spring Boot 4 基于 Jakarta EE 11 / Servlet 6.1，须使用 Tomcat 11，10.1.x 仅支持 Servlet 6.0 不兼容）。
 * 继承 {@link SpringBootServletInitializer} 以支持外部 Tomcat War 部署。
 *
 * @author hongfu_zhou@cacch.com
 */
@SpringBootApplication
public class CacchEnterpriseApplication extends SpringBootServletInitializer {

    /**
     * War 部署入口：外部 Tomcat 通过该方法加载本应用上下文
     *
     * @param builder Spring 应用构建器
     * @return 配置了启动类的应用构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CacchEnterpriseApplication.class);
    }

    /**
     * 本地开发启动入口（内嵌容器）
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CacchEnterpriseApplication.class, args);
    }
}
