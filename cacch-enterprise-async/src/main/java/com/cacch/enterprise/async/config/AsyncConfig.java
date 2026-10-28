package com.cacch.enterprise.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行基础设施配置
 *
 * <p>事件监听器统一使用独立线程池，避免阻塞主流程；
 * 队列满时由调用线程执行（CallerRunsPolicy），保证登录事件不丢失。
 *
 * @author hongfu_zhou@cacch.com
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 事件监听线程池
     *
     * @return 事件执行器，@Async("eventExecutor") 引用
     */
    @Bean("eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
