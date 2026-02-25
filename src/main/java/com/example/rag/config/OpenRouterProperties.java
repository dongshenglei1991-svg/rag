package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenRouter API 配置属性
 * 从 application.yml 中读取 openrouter 配置
 */
@Component
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {
    
    /**
     * OpenRouter API Key
     */
    private String apiKey;
    
    /**
     * OpenRouter API 基础 URL
     * 默认: https://openrouter.ai/api/v1
     */
    private String baseUrl = "https://openrouter.ai/api/v1";
    
    /**
     * 向量化模型
     * 默认: openai/text-embedding-3-small
     */
    private String embeddingModel = "openai/text-embedding-3-small";
    
    /**
     * 对话模型
     * 默认: openai/gpt-4
     */
    private String chatModel = "openai/gpt-4";
    
    /**
     * 请求超时时间（毫秒）
     * 默认: 30000 (30秒)
     */
    private Integer timeout = 30000;
    
    /**
     * 最大重试次数
     * 默认: 3
     */
    private Integer maxRetries = 3;
    
    /**
     * HTTP 代理主机
     */
    private String proxyHost;
    
    /**
     * HTTP 代理端口
     */
    private Integer proxyPort;
    
    // Getters and Setters
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getEmbeddingModel() {
        return embeddingModel;
    }
    
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    public String getChatModel() {
        return chatModel;
    }
    
    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }
    
    public Integer getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
}
