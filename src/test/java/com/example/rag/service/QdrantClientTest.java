package com.example.rag.service;

import com.example.rag.config.QdrantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qdrant 客户端测试
 * 验证 Qdrant 连接和基本操作
 */
@SpringBootTest
class QdrantClientTest {
    
    @Autowired
    private QdrantClient qdrantClient;
    
    @Autowired
    private QdrantProperties qdrantProperties;
    
    @BeforeEach
    void setUp() {
        assertNotNull(qdrantClient, "QdrantClient should be autowired");
        assertNotNull(qdrantProperties, "QdrantProperties should be autowired");
    }
    
    /**
     * 测试 Qdrant 连接
     */
    @Test
    void testConnection() {
        boolean connected = qdrantClient.testConnection();
        assertTrue(connected, "Should be able to connect to Qdrant");
    }
    
    /**
     * 测试 Collection 存在性检查
     */
    @Test
    void testCollectionExists() {
        String collectionName = qdrantProperties.getCollectionName();
        boolean exists = qdrantClient.collectionExists(collectionName);
        
        // Collection 应该在应用启动时被创建
        assertTrue(exists, "Collection '" + collectionName + "' should exist after initialization");
    }
    
    /**
     * 测试获取 Collection 信息
     */
    @Test
    void testGetCollectionInfo() {
        String collectionName = qdrantProperties.getCollectionName();
        String info = qdrantClient.getCollectionInfo(collectionName);
        
        assertNotNull(info, "Collection info should not be null");
        assertTrue(info.contains("vectors"), "Collection info should contain vectors configuration");
        assertTrue(info.contains("1536"), "Collection should have dimension 1536");
        assertTrue(info.contains("Cosine"), "Collection should use Cosine distance");
    }
    
    /**
     * 测试列出所有 Collections
     */
    @Test
    void testListCollections() {
        String collections = qdrantClient.listCollections();
        
        assertNotNull(collections, "Collections list should not be null");
        assertTrue(collections.contains(qdrantProperties.getCollectionName()), 
                "Collections list should contain the configured collection");
    }
    
    /**
     * 测试创建和删除临时 Collection
     */
    @Test
    void testCreateAndDeleteCollection() {
        String testCollectionName = "test_collection_" + System.currentTimeMillis();
        
        try {
            // 创建测试 Collection
            boolean created = qdrantClient.createCollection(testCollectionName, 128, "Cosine");
            assertTrue(created, "Should be able to create test collection");
            
            // 验证 Collection 存在
            boolean exists = qdrantClient.collectionExists(testCollectionName);
            assertTrue(exists, "Test collection should exist after creation");
            
            // 获取 Collection 信息
            String info = qdrantClient.getCollectionInfo(testCollectionName);
            assertNotNull(info, "Test collection info should not be null");
            assertTrue(info.contains("128"), "Test collection should have dimension 128");
            
        } finally {
            // 清理：删除测试 Collection
            boolean deleted = qdrantClient.deleteCollection(testCollectionName);
            assertTrue(deleted, "Should be able to delete test collection");
            
            // 验证 Collection 已删除
            boolean exists = qdrantClient.collectionExists(testCollectionName);
            assertFalse(exists, "Test collection should not exist after deletion");
        }
    }
    
    /**
     * 测试配置属性
     */
    @Test
    void testQdrantProperties() {
        assertEquals("192.168.14.128", qdrantProperties.getHost(), "Host should match configuration");
        assertEquals(6333, qdrantProperties.getPort(), "Port should match configuration");
        assertEquals("document_chunks", qdrantProperties.getCollectionName(), "Collection name should match configuration");
        assertFalse(qdrantProperties.getUseGrpc(), "Should use REST API, not gRPC");
        
        String expectedBaseUrl = "http://192.168.14.128:6333";
        assertEquals(expectedBaseUrl, qdrantProperties.getBaseUrl(), "Base URL should be correctly formatted");
    }
}
