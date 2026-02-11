package com.example.rag.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.rag.config.QdrantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Qdrant 客户端服务
 * 封装与 Qdrant REST API 的交互
 */
@Service
public class QdrantClient {
    
    private static final Logger log = LoggerFactory.getLogger(QdrantClient.class);
    
    private final WebClient qdrantWebClient;
    private final QdrantProperties qdrantProperties;
    
    public QdrantClient(@Qualifier("qdrantWebClient") WebClient qdrantWebClient, 
                        QdrantProperties qdrantProperties) {
        this.qdrantWebClient = qdrantWebClient;
        this.qdrantProperties = qdrantProperties;
    }
    
    /**
     * 检查 Collection 是否存在
     * 
     * @param collectionName Collection 名称
     * @return 是否存在
     */
    public boolean collectionExists(String collectionName) {
        try {
            String response = qdrantWebClient.get()
                    .uri("/collections/{collection_name}", collectionName)
                    .retrieve()
                    .onStatus(
                            status -> status.equals(HttpStatus.NOT_FOUND),
                            clientResponse -> Mono.empty()
                    )
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                log.debug("Collection '{}' exists", collectionName);
                return true;
            }
            
            log.debug("Collection '{}' does not exist", collectionName);
            return false;
        } catch (Exception e) {
            log.warn("Error checking collection existence: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建 Collection
     * 
     * @param collectionName Collection 名称
     * @param vectorSize 向量维度
     * @param distance 距离度量方式（Cosine, Euclid, Dot）
     * @return 是否创建成功
     */
    public boolean createCollection(String collectionName, int vectorSize, String distance) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> vectors = new HashMap<>();
            vectors.put("size", vectorSize);
            vectors.put("distance", distance);
            requestBody.put("vectors", vectors);
            
            String requestJson = JSONUtil.toJsonStr(requestBody);
            log.debug("Creating collection '{}' with request: {}", collectionName, requestJson);
            
            String response = qdrantWebClient.put()
                    .uri("/collections/{collection_name}", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Collection '{}' created successfully. Response: {}", collectionName, response);
            return true;
        } catch (Exception e) {
            log.error("Failed to create collection '{}': {}", collectionName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取 Collection 信息
     * 
     * @param collectionName Collection 名称
     * @return Collection 信息的 JSON 字符串
     */
    public String getCollectionInfo(String collectionName) {
        try {
            String response = qdrantWebClient.get()
                    .uri("/collections/{collection_name}", collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.debug("Collection '{}' info: {}", collectionName, response);
            return response;
        } catch (Exception e) {
            log.error("Failed to get collection info for '{}': {}", collectionName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 删除 Collection
     * 
     * @param collectionName Collection 名称
     * @return 是否删除成功
     */
    public boolean deleteCollection(String collectionName) {
        try {
            String response = qdrantWebClient.delete()
                    .uri("/collections/{collection_name}", collectionName)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Collection '{}' deleted successfully. Response: {}", collectionName, response);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete collection '{}': {}", collectionName, e.getMessage());
            return false;
        }
    }
    
    /**
     * 测试 Qdrant 连接
     * 
     * @return 是否连接成功
     */
    public boolean testConnection() {
        try {
            String response = qdrantWebClient.get()
                    .uri("/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Qdrant connection test successful. Response: {}", response);
            return true;
        } catch (Exception e) {
            log.error("Qdrant connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有 Collections
     * 
     * @return Collections 列表的 JSON 字符串
     */
    public String listCollections() {
        try {
            String response = qdrantWebClient.get()
                    .uri("/collections")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.debug("Collections list: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to list collections: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据过滤条件删除向量点
     * 
     * @param collectionName Collection 名称
     * @param documentId 文档ID（用于过滤）
     * @return 是否删除成功
     */
    public boolean deletePointsByDocumentId(String collectionName, Long documentId) {
        try {
            // 构建删除请求体，使用 filter 条件
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> must = new HashMap<>();
            Map<String, Object> key = new HashMap<>();
            key.put("key", "document_id");
            key.put("match", Map.of("value", documentId));
            must.put("must", new Object[]{key});
            filter.put("filter", must);
            requestBody.put("filter", must);
            
            String requestJson = JSONUtil.toJsonStr(requestBody);
            log.debug("Deleting points from collection '{}' with filter: {}", collectionName, requestJson);
            
            String response = qdrantWebClient.post()
                    .uri("/collections/{collection_name}/points/delete", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.info("Points deleted from collection '{}' for document_id={}. Response: {}", 
                    collectionName, documentId, response);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete points from collection '{}' for document_id={}: {}", 
                    collectionName, documentId, e.getMessage(), e);
            return false;
        }
    }
}
