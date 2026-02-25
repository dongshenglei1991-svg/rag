package com.example.rag.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenRouter API 客户端配置
 * 配置 WebClient 用于调用 OpenRouter API
 */
@Configuration
public class OpenRouterConfig {
    
    private static final Logger log = LoggerFactory.getLogger(OpenRouterConfig.class);
    
    private final OpenRouterProperties properties;
    
    public OpenRouterConfig(OpenRouterProperties properties) {
        this.properties = properties;
    }
    
    /**
     * 创建 OpenRouter WebClient Bean
     * 配置请求头、超时和重试策略
     */
    @Bean
    public WebClient openRouterWebClient() {
        // 配置 HTTP 客户端超时
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getTimeout(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getTimeout(), TimeUnit.MILLISECONDS)));
        
        // 配置 HTTP 代理
        String proxyHost = properties.getProxyHost();
        Integer proxyPort = properties.getProxyPort();
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null) {
            log.info("Using HTTP proxy: {}:{}", proxyHost, proxyPort);
            httpClient = httpClient.proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(proxyHost)
                    .port(proxyPort));
        }
        
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // 配置默认请求头
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("HTTP-Referer", "https://github.com/dongshenglei1991-svg/rag")  // OpenRouter 要求的 Referer
                .defaultHeader("X-Title", "RAG Retrieval System")  // OpenRouter 要求的应用标题
                .defaultHeader("Content-Type", "application/json")
                // 添加请求日志过滤器
                .filter(logRequest())
                // 添加响应日志过滤器
                .filter(logResponse())
                .build();
    }
    
    /**
     * 请求日志过滤器
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("OpenRouter Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> {
                    // 不记录敏感的 Authorization 头
                    if (!"Authorization".equalsIgnoreCase(name)) {
                        values.forEach(value -> log.debug("  {}: {}", name, value));
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * 响应日志过滤器
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("OpenRouter Response: Status {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
    
    /**
     * 创建重试策略
     * 用于服务层调用 API 时的重试逻辑
     */
    @Bean
    public Retry openRouterRetrySpec() {
        return Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying OpenRouter API call, attempt: {}, error: {}", 
                            retrySignal.totalRetries() + 1, 
                            retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("OpenRouter API retry exhausted after {} attempts", 
                            retrySignal.totalRetries());
                    return retrySignal.failure();
                });
    }
}
