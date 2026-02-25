package com.example.rag.config;

import com.example.rag.service.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Qdrant 初始化器
 * 应用启动时自动创建 Collection
 */
@Component
public class QdrantInitializer implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(QdrantInitializer.class);
    
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;
    
    public QdrantInitializer(QdrantClient qdrantClient, QdrantProperties qdrantProperties) {
        this.qdrantClient = qdrantClient;
        this.qdrantProperties = qdrantProperties;
    }
    
    /**
     * 向量维度（与 OpenAI text-embedding-3-small 匹配）
     */
    private static final int VECTOR_DIMENSION = 1536;
    
    /**
     * 距离度量方式（余弦相似度）
     */
    private static final String DISTANCE_METRIC = "Cosine";
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing Qdrant...");
        
        // 测试 Qdrant 连接
        if (!qdrantClient.testConnection()) {
            log.error("Failed to connect to Qdrant at {}. Please check if Qdrant is running.", 
                    qdrantProperties.getBaseUrl());
            throw new RuntimeException("Qdrant connection failed");
        }
        
        log.info("Qdrant connection successful");
        
        // 检查 Collection 是否存在
        String collectionName = qdrantProperties.getCollectionName();
        if (qdrantClient.collectionExists(collectionName)) {
            log.info("Collection '{}' already exists", collectionName);
            
            // 获取并打印 Collection 信息
            String collectionInfo = qdrantClient.getCollectionInfo(collectionName);
            log.debug("Collection info: {}", collectionInfo);
        } else {
            log.info("Collection '{}' does not exist, creating...", collectionName);
            
            // 创建 Collection
            boolean created = qdrantClient.createCollection(
                    collectionName, 
                    VECTOR_DIMENSION, 
                    DISTANCE_METRIC
            );
            
            if (created) {
                log.info("Collection '{}' created successfully with dimension {} and distance metric {}", 
                        collectionName, VECTOR_DIMENSION, DISTANCE_METRIC);
            } else {
                log.error("Failed to create collection '{}'", collectionName);
                throw new RuntimeException("Failed to create Qdrant collection");
            }
        }
        
        // 列出所有 Collections
        String collections = qdrantClient.listCollections();
        log.info("Available collections: {}", collections);
        
        log.info("Qdrant initialization completed successfully");
    }
}
