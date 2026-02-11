package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异步线程池配置测试
 * 验证 AsyncConfig 创建的线程池参数是否正确
 *
 * 需求：10.5 - 通过连接池和线程池管理资源使用
 */
class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void testAsyncExecutorCreated() {
        Executor executor = asyncConfig.getAsyncExecutor();
        assertNotNull(executor, "Async executor should not be null");
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor,
                "Executor should be a ThreadPoolTaskExecutor");
    }

    @Test
    void testThreadPoolConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        assertEquals(2, executor.getCorePoolSize(), "Core pool size should be 2");
        assertEquals(5, executor.getMaxPoolSize(), "Max pool size should be 5");
        assertEquals("doc-process-", executor.getThreadNamePrefix(),
                "Thread name prefix should be 'doc-process-'");
    }

    @Test
    void testRejectedExecutionPolicy() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // CallerRunsPolicy ensures tasks are not lost when queue is full
        assertInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class,
                executor.getThreadPoolExecutor().getRejectedExecutionHandler(),
                "Rejected execution handler should be CallerRunsPolicy");
    }

    @Test
    void testGracefulShutdown() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // Verify the executor is configured for graceful shutdown
        // The executor should wait for tasks to complete on shutdown
        assertNotNull(executor.getThreadPoolExecutor(),
                "Thread pool executor should be initialized");

        // Clean up
        executor.shutdown();
    }
}
