package com.example.rag.config;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * Qdrant 配置类
 * 创建用于与 Qdrant REST API 通信的 WebClient Bean
 */
@Configuration
public class QdrantConfig {
    
    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);
    
    private final QdrantProperties qdrantProperties;
    
    public QdrantConfig(QdrantProperties qdrantProperties) {
        this.qdrantProperties = qdrantProperties;
    }
    
    /**
     * 创建 Qdrant WebClient Bean
     * 用于调用 Qdrant REST API
     * 配置连接池以优化向量检索性能
     *
     * 需求：10.4 - 实现连接池和自动重连机制
     */
    @Bean(name = "qdrantWebClient")
    public WebClient qdrantWebClient() {
        // 配置连接池，复用 TCP 连接以提升向量检索性能
        ConnectionProvider connectionProvider = ConnectionProvider.builder("qdrant-pool")
                .maxConnections(20)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireMaxCount(50)
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .build();

        // 配置 HTTP 客户端超时和连接池
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> log.debug("Connected to Qdrant at {}", qdrantProperties.getBaseUrl()));
        
        WebClient webClient = WebClient.builder()
                .baseUrl(qdrantProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        log.info("Qdrant WebClient configured with connection pool (maxConnections=20) and base URL: {}", 
                qdrantProperties.getBaseUrl());
        
        return webClient;
    }
}
