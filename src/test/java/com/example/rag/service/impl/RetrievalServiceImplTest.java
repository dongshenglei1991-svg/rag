package com.example.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.rag.config.RagRetrievalProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentChunkMapper;
import com.example.rag.service.EmbeddingService;
import com.example.rag.service.RetrievalService;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.RetrievalResult;
import com.example.rag.vo.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RetrievalServiceImpl 单元测试
 *
 * 需求：4.1 - 将查询文本向量化
 * 需求：4.2 - 在向量数据库中执行相似度搜索
 * 需求：4.3 - 返回前 K 个最相关的文档片段
 * 需求：4.4 - 包含每个片段的相似度分数和原始文本内容
 */
@ExtendWith(MockitoExtension.class)
class RetrievalServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    private RagRetrievalProperties ragRetrievalProperties;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        ragRetrievalProperties = new RagRetrievalProperties();
        ragRetrievalProperties.setTopK(5);
        ragRetrievalProperties.setMinScore(0.7);

        retrievalService = new RetrievalServiceImpl(
                embeddingService,
                vectorStoreService,
                documentChunkMapper,
                ragRetrievalProperties
        );
    }

    // ==================== Successful Retrieval Tests ====================

    @Test
    void testRetrieve_Success() throws Exception {
        // Arrange
        String query = "什么是民事权利能力？";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        List<SearchResult> searchResults = new ArrayList<>();
        Map<String, Object> payload1 = new LinkedHashMap<>();
        payload1.put("document_id", 1);
        payload1.put("content", "民法典第十三条内容");
        payload1.put("document_name", "民法典.pdf");
        searchResults.add(new SearchResult("vector-uuid-1", 0.95f, payload1));

        Map<String, Object> payload2 = new LinkedHashMap<>();
        payload2.put("document_id", 1);
        payload2.put("content", "民法典第十四条内容");
        payload2.put("document_name", "民法典.pdf");
        searchResults.add(new SearchResult("vector-uuid-2", 0.85f, payload2));

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);

        DocumentChunk chunk1 = createDocumentChunk(1L, 1L, 0, "民法典第十三条内容", "vector-uuid-1");
        DocumentChunk chunk2 = createDocumentChunk(2L, 1L, 1, "民法典第十四条内容", "vector-uuid-2");

        when(documentChunkMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(chunk1)
                .thenReturn(chunk2);

        // Act
        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // Results should be sorted by score descending
        assertEquals(0.95f, results.get(0).getScore(), 0.01f);
        assertEquals(0.85f, results.get(1).getScore(), 0.01f);

        // Verify chunk data
        assertEquals("民法典第十三条内容", results.get(0).getChunk().getContent());
        assertEquals("民法典.pdf", results.get(0).getDocumentName());
        assertEquals("vector-uuid-1", results.get(0).getChunk().getVectorId());

        assertEquals("民法典第十四条内容", results.get(1).getChunk().getContent());
        assertEquals("民法典.pdf", results.get(1).getDocumentName());

        // Verify interactions
        verify(embeddingService).embed(query);
        verify(vectorStoreService).search(queryVector, 5);
        verify(documentChunkMapper, times(2)).selectOne(any(QueryWrapper.class));
    }

    @Test
    void testRetrieve_ResultsSortedByScoreDescending() throws Exception {
        // Arrange - search results come in non-sorted order
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult("uuid-1", 0.70f, Map.of("document_name", "doc1.pdf")));
        searchResults.add(new SearchResult("uuid-2", 0.95f, Map.of("document_name", "doc2.pdf")));
        searchResults.add(new SearchResult("uuid-3", 0.80f, Map.of("document_name", "doc3.pdf")));

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);

        DocumentChunk chunk1 = createDocumentChunk(1L, 1L, 0, "内容1", "uuid-1");
        DocumentChunk chunk2 = createDocumentChunk(2L, 2L, 0, "内容2", "uuid-2");
        DocumentChunk chunk3 = createDocumentChunk(3L, 3L, 0, "内容3", "uuid-3");

        when(documentChunkMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(chunk1)
                .thenReturn(chunk2)
                .thenReturn(chunk3);

        // Act
        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        // Assert - should be sorted by score descending
        assertEquals(3, results.size());
        assertEquals(0.95f, results.get(0).getScore(), 0.01f);
        assertEquals(0.80f, results.get(1).getScore(), 0.01f);
        assertEquals(0.70f, results.get(2).getScore(), 0.01f);
    }

    // ==================== Empty/Null Query Tests ====================

    @Test
    void testRetrieve_NullQuery() throws Exception {
        List<RetrievalResult> results = retrievalService.retrieve(null, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingService, vectorStoreService, documentChunkMapper);
    }

    @Test
    void testRetrieve_EmptyQuery() throws Exception {
        List<RetrievalResult> results = retrievalService.retrieve("", 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingService, vectorStoreService, documentChunkMapper);
    }

    @Test
    void testRetrieve_BlankQuery() throws Exception {
        List<RetrievalResult> results = retrievalService.retrieve("   ", 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(embeddingService, vectorStoreService, documentChunkMapper);
    }

    // ==================== Empty Search Results Tests ====================

    @Test
    void testRetrieve_NoSearchResults() throws Exception {
        String query = "不存在的内容";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);
        when(vectorStoreService.search(queryVector, 5)).thenReturn(Collections.emptyList());

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(embeddingService).embed(query);
        verify(vectorStoreService).search(queryVector, 5);
        verifyNoInteractions(documentChunkMapper);
    }

    @Test
    void testRetrieve_NullSearchResults() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);
        when(vectorStoreService.search(queryVector, 5)).thenReturn(null);

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== Missing DocumentChunk Tests ====================

    @Test
    void testRetrieve_DocumentChunkNotFoundInDB() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult("uuid-1", 0.95f, Map.of("document_name", "doc.pdf")));
        searchResults.add(new SearchResult("uuid-missing", 0.85f, Map.of("document_name", "doc.pdf")));

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);

        DocumentChunk chunk1 = createDocumentChunk(1L, 1L, 0, "内容1", "uuid-1");

        // First call returns chunk, second returns null (not found)
        when(documentChunkMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(chunk1)
                .thenReturn(null);

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        // Should only contain the one that was found
        assertEquals(1, results.size());
        assertEquals("内容1", results.get(0).getChunk().getContent());
        assertEquals(0.95f, results.get(0).getScore(), 0.01f);
    }

    @Test
    void testRetrieve_AllDocumentChunksNotFound() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult("uuid-missing-1", 0.95f, Map.of("document_name", "doc.pdf")));
        searchResults.add(new SearchResult("uuid-missing-2", 0.85f, Map.of("document_name", "doc.pdf")));

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);
        when(documentChunkMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== TopK Configuration Tests ====================

    @Test
    void testRetrieve_UsesProvidedTopK() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);
        when(vectorStoreService.search(queryVector, 3)).thenReturn(Collections.emptyList());

        retrievalService.retrieve(query, 3);

        verify(vectorStoreService).search(queryVector, 3);
    }

    @Test
    void testRetrieve_UsesConfiguredTopKWhenZero() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);
        when(vectorStoreService.search(queryVector, 5)).thenReturn(Collections.emptyList());

        // topK = 0 should fall back to configured value (5)
        retrievalService.retrieve(query, 0);

        verify(vectorStoreService).search(queryVector, 5);
    }

    @Test
    void testRetrieve_UsesConfiguredTopKWhenNegative() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);
        when(vectorStoreService.search(queryVector, 5)).thenReturn(Collections.emptyList());

        // topK = -1 should fall back to configured value (5)
        retrievalService.retrieve(query, -1);

        verify(vectorStoreService).search(queryVector, 5);
    }

    // ==================== Document Name Extraction Tests ====================

    @Test
    void testRetrieve_ExtractsDocumentNameFromPayload() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("document_name", "民法典.pdf");
        payload.put("document_id", 1);

        List<SearchResult> searchResults = List.of(
                new SearchResult("uuid-1", 0.90f, payload)
        );

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);

        DocumentChunk chunk = createDocumentChunk(1L, 1L, 0, "内容", "uuid-1");
        when(documentChunkMapper.selectOne(any(QueryWrapper.class))).thenReturn(chunk);

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        assertEquals(1, results.size());
        assertEquals("民法典.pdf", results.get(0).getDocumentName());
    }

    @Test
    void testRetrieve_NullPayload() throws Exception {
        String query = "测试查询";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};

        when(embeddingService.embed(query)).thenReturn(queryVector);

        List<SearchResult> searchResults = List.of(
                new SearchResult("uuid-1", 0.90f, null)
        );

        when(vectorStoreService.search(queryVector, 5)).thenReturn(searchResults);

        DocumentChunk chunk = createDocumentChunk(1L, 1L, 0, "内容", "uuid-1");
        when(documentChunkMapper.selectOne(any(QueryWrapper.class))).thenReturn(chunk);

        List<RetrievalResult> results = retrievalService.retrieve(query, 5);

        assertEquals(1, results.size());
        assertNull(results.get(0).getDocumentName());
        assertNotNull(results.get(0).getChunk());
    }

    // ==================== Exception Propagation Tests ====================

    @Test
    void testRetrieve_EmbeddingServiceThrowsException() {
        String query = "测试查询";

        assertThrows(Exception.class, () -> {
            when(embeddingService.embed(query)).thenThrow(new RuntimeException("Embedding API failed"));
            retrievalService.retrieve(query, 5);
        });

        verifyNoInteractions(vectorStoreService, documentChunkMapper);
    }

    // ==================== Helper Methods ====================

    private DocumentChunk createDocumentChunk(Long id, Long documentId, Integer chunkIndex,
                                               String content, String vectorId) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setVectorId(vectorId);
        chunk.setCharCount(content.length());
        chunk.setCreatedAt(LocalDateTime.now());
        return chunk;
    }
}
