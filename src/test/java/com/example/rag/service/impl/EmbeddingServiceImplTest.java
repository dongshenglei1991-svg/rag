package com.example.rag.service.impl;

import com.example.rag.config.OpenRouterProperties;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.EmbeddingService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingService 单元测试
 * 使用 MockWebServer 模拟 OpenRouter Embedding API
 */
class EmbeddingServiceImplTest {
    
    private MockWebServer mockWebServer;
    private EmbeddingService embeddingService;
    private OpenRouterProperties properties;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        properties = new OpenRouterProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl(mockWebServer.url("/").toString());
        properties.setEmbeddingModel("openai/text-embedding-3-small");
        properties.setTimeout(5000);
        properties.setMaxRetries(3);
        
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        embeddingService = new EmbeddingServiceImpl(webClient, properties);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void testEmbed_Success() throws Exception {
        String responseBody = """
                {
                    "object": "list",
                    "data": [
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 0
                        }
                    ],
                    "model": "openai/text-embedding-3-small",
                    "usage": {
                        "prompt_tokens": 5,
                        "total_tokens": 5
                    }
                }
                """.formatted(generateMockEmbedding(1536));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        float[] result = embeddingService.embed("测试文本");
        
        assertNotNull(result);
        assertEquals(1536, result.length);
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/embeddings", request.getPath());
    }
    
    @Test
    void testEmbed_EmptyText() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embed("");
        });
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文本内容不能为空"));
    }
    
    @Test
    void testEmbed_NullText() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embed(null);
        });
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文本内容不能为空"));
    }
    
    @Test
    void testEmbed_ApiError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embed("测试文本");
        });
        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("向量化服务调用失败"));
    }
    
    @Test
    void testEmbed_EmptyResponse() {
        String responseBody = """
                {
                    "object": "list",
                    "data": [],
                    "model": "openai/text-embedding-3-small"
                }
                """;
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embed("测试文本");
        });
        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("向量化服务返回空结果"));
    }
    
    @Test
    void testEmbed_WrongDimension() {
        String responseBody = """
                {
                    "object": "list",
                    "data": [
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 0
                        }
                    ],
                    "model": "openai/text-embedding-3-small"
                }
                """.formatted(generateMockEmbedding(512));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embed("测试文本");
        });
        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("向量维度不正确"));
    }
    
    @Test
    void testEmbedBatch_Success() throws Exception {
        String responseBody = """
                {
                    "object": "list",
                    "data": [
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 0
                        },
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 1
                        }
                    ],
                    "model": "openai/text-embedding-3-small",
                    "usage": {
                        "prompt_tokens": 10,
                        "total_tokens": 10
                    }
                }
                """.formatted(generateMockEmbedding(1536), generateMockEmbedding(1536));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        List<String> texts = Arrays.asList("文本1", "文本2");
        List<float[]> results = embeddingService.embedBatch(texts);
        
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(1536, results.get(0).length);
        assertEquals(1536, results.get(1).length);
    }
    
    @Test
    void testEmbedBatch_EmptyList() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embedBatch(Arrays.asList());
        });
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文本列表不能为空"));
    }
    
    @Test
    void testEmbedBatch_NullList() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embedBatch(null);
        });
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文本列表不能为空"));
    }
    
    @Test
    void testEmbedBatch_CountMismatch() {
        String responseBody = """
                {
                    "object": "list",
                    "data": [
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 0
                        }
                    ],
                    "model": "openai/text-embedding-3-small"
                }
                """.formatted(generateMockEmbedding(1536));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            embeddingService.embedBatch(Arrays.asList("文本1", "文本2"));
        });
        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("向量化结果数量不匹配"));
    }
    
    @Test
    void testEmbed_RetryMechanism() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        
        String responseBody = """
                {
                    "object": "list",
                    "data": [
                        {
                            "object": "embedding",
                            "embedding": %s,
                            "index": 0
                        }
                    ],
                    "model": "openai/text-embedding-3-small"
                }
                """.formatted(generateMockEmbedding(1536));
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));
        
        float[] result = embeddingService.embed("测试文本");
        
        assertNotNull(result);
        assertEquals(1536, result.length);
        assertEquals(3, mockWebServer.getRequestCount());
    }
    
    @Test
    void testGetDimension() {
        assertEquals(1536, embeddingService.getDimension());
    }
    
    private String generateMockEmbedding(int dimension) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dimension; i++) {
            if (i > 0) sb.append(",");
            sb.append(Math.random());
        }
        sb.append("]");
        return sb.toString();
    }
}
