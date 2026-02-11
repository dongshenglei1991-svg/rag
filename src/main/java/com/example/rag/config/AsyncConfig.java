package com.example.rag.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * 为文档处理等异步任务提供自定义线程池，替代 Spring 默认的 SimpleAsyncTaskExecutor
 *
 * 需求：10.5 - 通过连接池和线程池管理资源使用
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 配置文档处理异步线程池
     * - corePoolSize: 核心线程数，保持活跃处理文档
     * - maxPoolSize: 最大线程数，应对突发上传
     * - queueCapacity: 队列容量，缓冲待处理任务
     * - CallerRunsPolicy: 队列满时由调用线程执行，避免任务丢失
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("doc-process-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Async thread pool configured: corePoolSize=2, maxPoolSize=5, queueCapacity=20");
        return executor;
    }
}
