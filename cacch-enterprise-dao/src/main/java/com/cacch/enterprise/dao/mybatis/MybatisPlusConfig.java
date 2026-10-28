package com.cacch.enterprise.dao.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * <p>数据权限拦截器在前、分页拦截器在后：数据权限条件先追加再分页，保证 count 准确。
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@MapperScan("com.cacch.enterprise.dao.mapper")
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 内部拦截器链
     *
     * @param dataPermissionHandler 数据权限处理器（追加部门过滤条件）
     * @return 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(SysDataPermissionHandler dataPermissionHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
