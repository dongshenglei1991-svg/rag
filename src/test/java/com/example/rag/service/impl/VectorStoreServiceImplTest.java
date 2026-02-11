package com.example.rag.service.impl;

import com.example.rag.config.QdrantProperties;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.SearchResult;
import com.example.rag.vo.VectorPoint;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VectorStoreService 单元测试
 * 使用 MockWebServer 模拟 Qdrant REST API
 */
class VectorStoreServiceImplTest {

    private MockWebServer mockWebServer;
    private VectorStoreService vectorStoreService;
    private QdrantProperties qdrantProperties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        qdrantProperties = new QdrantProperties();
        qdrantProperties.setHost("localhost");
        qdrantProperties.setPort(mockWebServer.getPort());
        qdrantProperties.setCollectionName("document_chunks");

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        vectorStoreService = new VectorStoreServiceImpl(webClient, qdrantProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ==================== storeVector Tests ====================

    @Test
    void testStoreVector_Success() throws InterruptedException {
        String qdrantResponse = """
                {
                    "result": {
                        "operation_id": 1,
                        "status": "completed"
                    },
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("document_id", 1L);
        metadata.put("chunk_id", 100L);
        metadata.put("chunk_index", 0);
        metadata.put("content", "测试文本内容");
        metadata.put("document_name", "test.pdf");

        assertDoesNotThrow(() ->
                vectorStoreService.storeVector("test-uuid-1", vector, metadata));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertTrue(request.getPath().contains("/collections/document_chunks/points"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("test-uuid-1"));
        assertTrue(body.contains("document_id"));
        assertTrue(body.contains("测试文本内容"));
    }

    @Test
    void testStoreVector_NullId() {
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = Map.of("document_id", 1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.storeVector(null, vector, metadata));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量ID不能为空"));
    }

    @Test
    void testStoreVector_EmptyId() {
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = Map.of("document_id", 1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.storeVector("", vector, metadata));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量ID不能为空"));
    }

    @Test
    void testStoreVector_NullVector() {
        Map<String, Object> metadata = Map.of("document_id", 1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.storeVector("test-uuid", null, metadata));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量数据不能为空"));
    }

    @Test
    void testStoreVector_EmptyVector() {
        Map<String, Object> metadata = Map.of("document_id", 1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.storeVector("test-uuid", new float[]{}, metadata));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量数据不能为空"));
    }

    // ==================== storeVectorBatch Tests ====================

    @Test
    void testStoreVectorBatch_Success() throws InterruptedException {
        String qdrantResponse = """
                {
                    "result": {
                        "operation_id": 1,
                        "status": "completed"
                    },
                    "status": "ok",
                    "time": 0.002
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        List<VectorPoint> points = new ArrayList<>();

        Map<String, Object> metadata1 = new LinkedHashMap<>();
        metadata1.put("document_id", 1L);
        metadata1.put("chunk_id", 100L);
        metadata1.put("chunk_index", 0);
        metadata1.put("content", "第一个片段");
        metadata1.put("document_name", "test.pdf");
        points.add(new VectorPoint("uuid-1", new float[]{0.1f, 0.2f, 0.3f}, metadata1));

        Map<String, Object> metadata2 = new LinkedHashMap<>();
        metadata2.put("document_id", 1L);
        metadata2.put("chunk_id", 101L);
        metadata2.put("chunk_index", 1);
        metadata2.put("content", "第二个片段");
        metadata2.put("document_name", "test.pdf");
        points.add(new VectorPoint("uuid-2", new float[]{0.4f, 0.5f, 0.6f}, metadata2));

        assertDoesNotThrow(() -> vectorStoreService.storeVectorBatch(points));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertTrue(request.getPath().contains("/collections/document_chunks/points"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("uuid-1"));
        assertTrue(body.contains("uuid-2"));
        assertTrue(body.contains("第一个片段"));
        assertTrue(body.contains("第二个片段"));
    }

    @Test
    void testStoreVectorBatch_EmptyList() {
        // Should not throw, just log a warning and return
        assertDoesNotThrow(() -> vectorStoreService.storeVectorBatch(new ArrayList<>()));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testStoreVectorBatch_NullList() {
        assertDoesNotThrow(() -> vectorStoreService.storeVectorBatch(null));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testStoreVectorBatch_QdrantError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        List<VectorPoint> points = List.of(
                new VectorPoint("uuid-1", new float[]{0.1f}, Map.of("document_id", 1L))
        );

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.storeVectorBatch(points));

        assertEquals(500, exception.getCode());
        assertTrue(exception.getMessage().contains("向量存储失败"));
    }

    // ==================== search Tests ====================

    @Test
    void testSearch_Success() {
        String qdrantResponse = """
                {
                    "result": [
                        {
                            "id": "uuid-1",
                            "version": 1,
                            "score": 0.95,
                            "payload": {
                                "document_id": 1,
                                "chunk_id": 100,
                                "chunk_index": 0,
                                "content": "相关文本内容",
                                "document_name": "test.pdf"
                            }
                        },
                        {
                            "id": "uuid-2",
                            "version": 1,
                            "score": 0.85,
                            "payload": {
                                "document_id": 1,
                                "chunk_id": 101,
                                "chunk_index": 1,
                                "content": "另一段相关内容",
                                "document_name": "test.pdf"
                            }
                        }
                    ],
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};
        List<SearchResult> results = vectorStoreService.search(queryVector, 5);

        assertNotNull(results);
        assertEquals(2, results.size());

        SearchResult first = results.get(0);
        assertEquals("uuid-1", first.getId());
        assertEquals(0.95f, first.getScore(), 0.01f);
        assertNotNull(first.getPayload());
        assertEquals("相关文本内容", first.getPayload().get("content"));
        assertEquals("test.pdf", first.getPayload().get("document_name"));

        SearchResult second = results.get(1);
        assertEquals("uuid-2", second.getId());
        assertEquals(0.85f, second.getScore(), 0.01f);
    }

    @Test
    void testSearch_EmptyResults() {
        String qdrantResponse = """
                {
                    "result": [],
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};
        List<SearchResult> results = vectorStoreService.search(queryVector, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearch_NullQueryVector() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.search(null, 5));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("查询向量不能为空"));
    }

    @Test
    void testSearch_EmptyQueryVector() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.search(new float[]{}, 5));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("查询向量不能为空"));
    }

    @Test
    void testSearch_InvalidTopK() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.search(new float[]{0.1f}, 0));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("topK 必须大于 0"));
    }

    // ==================== deleteVector Tests ====================

    @Test
    void testDeleteVector_Success() throws InterruptedException {
        String qdrantResponse = """
                {
                    "result": {
                        "operation_id": 2,
                        "status": "completed"
                    },
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        assertDoesNotThrow(() -> vectorStoreService.deleteVector("test-uuid-1"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/collections/document_chunks/points/delete"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("test-uuid-1"));
    }

    @Test
    void testDeleteVector_NullId() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.deleteVector(null));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量ID不能为空"));
    }

    @Test
    void testDeleteVector_EmptyId() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.deleteVector(""));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("向量ID不能为空"));
    }

    // ==================== deleteByDocumentId Tests ====================

    @Test
    void testDeleteByDocumentId_Success() throws InterruptedException {
        String qdrantResponse = """
                {
                    "result": {
                        "operation_id": 3,
                        "status": "completed"
                    },
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        assertDoesNotThrow(() -> vectorStoreService.deleteByDocumentId(123L));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/collections/document_chunks/points/delete"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("document_id"));
        assertTrue(body.contains("123"));
    }

    @Test
    void testDeleteByDocumentId_NullId() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.deleteByDocumentId(null));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("文档ID不能为空"));
    }

    @Test
    void testDeleteByDocumentId_QdrantError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                vectorStoreService.deleteByDocumentId(123L));

        assertEquals(500, exception.getCode());
        assertTrue(exception.getMessage().contains("按文档ID删除向量失败"));
    }

    // ==================== Payload Verification Tests ====================

    @Test
    void testStoreVector_PayloadContainsRequiredFields() throws InterruptedException {
        String qdrantResponse = """
                {
                    "result": {
                        "operation_id": 1,
                        "status": "completed"
                    },
                    "status": "ok",
                    "time": 0.001
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(qdrantResponse));

        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("document_id", 42L);
        metadata.put("chunk_id", 200L);
        metadata.put("chunk_index", 3);
        metadata.put("content", "民法典第一条内容");
        metadata.put("document_name", "民法典.pdf");

        vectorStoreService.storeVector("payload-test-uuid", vector, metadata);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();

        // Verify all required payload fields are present
        assertTrue(body.contains("document_id"), "Payload should contain document_id");
        assertTrue(body.contains("chunk_id"), "Payload should contain chunk_id");
        assertTrue(body.contains("chunk_index"), "Payload should contain chunk_index");
        assertTrue(body.contains("content"), "Payload should contain content");
        assertTrue(body.contains("document_name"), "Payload should contain document_name");
        assertTrue(body.contains("42"), "Payload should contain document_id value");
        assertTrue(body.contains("民法典第一条内容"), "Payload should contain content value");
        assertTrue(body.contains("民法典.pdf"), "Payload should contain document_name value");
    }
}
