package com.example.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenRouter 配置测试
 * 验证 OpenRouter API 客户端配置是否正确
 */
@SpringBootTest
@TestPropertySource(properties = {
    "openrouter.api-key=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",
    "openrouter.base-url=https://openrouter.ai/api/v1",
    "openrouter.embedding-model=qwen/qwen3-embedding-0.6b",
    "openrouter.chat-model=openai/gpt-4",
    "openrouter.timeout=30000",
    "openrouter.max-retries=3"
})
class OpenRouterConfigTest {

    @Autowired
    private OpenRouterProperties properties;

    @Autowired
    private WebClient openRouterWebClient;

    @Autowired
    private Retry openRouterRetrySpec;

    /**
     * 测试 OpenRouterProperties 是否正确加载
     */
    @Test
    void testOpenRouterPropertiesLoaded() {
        assertNotNull(properties, "OpenRouterProperties should not be null");
        
        // 验证 API Key 已配置
        assertNotNull(properties.getApiKey(), "API Key should not be null");
        assertFalse(properties.getApiKey().isEmpty(), "API Key should not be empty");
        assertTrue(properties.getApiKey().startsWith("sk-or-v1-"), 
                "API Key should start with 'sk-or-v1-'");
        
        // 验证 Base URL
        assertEquals("https://openrouter.ai/api/v1", properties.getBaseUrl(), 
                "Base URL should be https://openrouter.ai/api/v1");
        
        // 验证模型配置
        assertEquals("qwen/qwen3-embedding-0.6b", properties.getEmbeddingModel(), 
                "Embedding model should be qwen/qwen3-embedding-0.6b");
        assertEquals("openai/gpt-4", properties.getChatModel(), 
                "Chat model should be openai/gpt-4");
        
        // 验证超时和重试配置
        assertEquals(30000, properties.getTimeout(), "Timeout should be 30000ms");
        assertEquals(3, properties.getMaxRetries(), "Max retries should be 3");
        
        System.out.println("✓ OpenRouter properties loaded successfully!");
        System.out.println("  Base URL: " + properties.getBaseUrl());
        System.out.println("  Embedding Model: " + properties.getEmbeddingModel());
        System.out.println("  Chat Model: " + properties.getChatModel());
        System.out.println("  Timeout: " + properties.getTimeout() + "ms");
        System.out.println("  Max Retries: " + properties.getMaxRetries());
    }

    /**
     * 测试 WebClient Bean 是否正确创建
     */
    @Test
    void testWebClientBeanCreated() {
        assertNotNull(openRouterWebClient, "OpenRouter WebClient should not be null");
        
        System.out.println("✓ OpenRouter WebClient bean created successfully!");
    }

    /**
     * 测试 Retry Bean 是否正确创建
     */
    @Test
    void testRetryBeanCreated() {
        assertNotNull(openRouterRetrySpec, "OpenRouter Retry spec should not be null");
        
        System.out.println("✓ OpenRouter Retry spec bean created successfully!");
    }

    /**
     * 测试配置的完整性
     */
    @Test
    void testConfigurationCompleteness() {
        // 验证所有必需的配置都已设置
        assertNotNull(properties.getApiKey(), "API Key must be configured");
        assertNotNull(properties.getBaseUrl(), "Base URL must be configured");
        assertNotNull(properties.getEmbeddingModel(), "Embedding model must be configured");
        assertNotNull(properties.getChatModel(), "Chat model must be configured");
        assertNotNull(properties.getTimeout(), "Timeout must be configured");
        assertNotNull(properties.getMaxRetries(), "Max retries must be configured");
        
        // 验证配置值的合理性
        assertTrue(properties.getTimeout() > 0, "Timeout should be positive");
        assertTrue(properties.getMaxRetries() > 0, "Max retries should be positive");
        assertTrue(properties.getMaxRetries() <= 5, "Max retries should not exceed 5");
        
        System.out.println("✓ OpenRouter configuration is complete and valid!");
    }

    /**
     * 测试 API Key 格式
     */
    @Test
    void testApiKeyFormat() {
        String apiKey = properties.getApiKey();
        
        // OpenRouter API Key 格式验证
        assertTrue(apiKey.startsWith("sk-or-v1-"), 
                "API Key should start with 'sk-or-v1-'");
        assertTrue(apiKey.length() > 20, 
                "API Key should be longer than 20 characters");
        
        System.out.println("✓ API Key format is valid!");
    }

    /**
     * 测试超时配置的合理性
     */
    @Test
    void testTimeoutConfiguration() {
        Integer timeout = properties.getTimeout();
        
        // 超时时间应该在合理范围内（5秒到60秒）
        assertTrue(timeout >= 5000, "Timeout should be at least 5 seconds");
        assertTrue(timeout <= 60000, "Timeout should not exceed 60 seconds");
        
        System.out.println("✓ Timeout configuration is reasonable: " + timeout + "ms");
    }

    /**
     * 测试重试配置的合理性
     */
    @Test
    void testRetryConfiguration() {
        Integer maxRetries = properties.getMaxRetries();
        
        // 重试次数应该在合理范围内（1到5次）
        assertTrue(maxRetries >= 1, "Max retries should be at least 1");
        assertTrue(maxRetries <= 5, "Max retries should not exceed 5");
        
        System.out.println("✓ Retry configuration is reasonable: " + maxRetries + " retries");
    }
}
