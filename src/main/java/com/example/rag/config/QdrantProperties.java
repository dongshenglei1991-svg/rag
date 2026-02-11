package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Qdrant 配置属性
 * 从 application.yml 中读取 qdrant 配置
 */
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {
    
    /**
     * Qdrant 服务器地址
     */
    private String host = "localhost";
    
    /**
     * REST API 端口
     */
    private Integer port = 6333;
    
    /**
     * gRPC 端口
     */
    private Integer grpcPort = 6334;
    
    /**
     * Collection 名称
     */
    private String collectionName = "document_chunks";
    
    /**
     * 是否使用 gRPC（false 表示使用 REST API）
     */
    private Boolean useGrpc = false;
    
    /**
     * 获取 REST API 基础 URL
     */
    public String getBaseUrl() {
        return String.format("http://%s:%d", host, port);
    }
    
    /**
     * 获取 Collection 名称
     */
    public String getCollectionName() {
        return collectionName;
    }
    
    /**
     * 获取主机地址
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 获取端口
     */
    public Integer getPort() {
        return port;
    }
    
    /**
     * 获取是否使用 gRPC
     */
    public Boolean getUseGrpc() {
        return useGrpc;
    }
    
    /**
     * 设置主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * 设置端口
     */
    public void setPort(Integer port) {
        this.port = port;
    }
    
    /**
     * 设置 gRPC 端口
     */
    public void setGrpcPort(Integer grpcPort) {
        this.grpcPort = grpcPort;
    }
    
    /**
     * 设置 Collection 名称
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
    
    /**
     * 设置是否使用 gRPC
     */
    public void setUseGrpc(Boolean useGrpc) {
        this.useGrpc = useGrpc;
    }
}
