package com.example.rag.service.impl;

import cn.hutool.json.JSONUtil;
import com.example.rag.config.RagRetrievalProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.entity.QueryHistory;
import com.example.rag.mapper.QueryHistoryMapper;
import com.example.rag.service.LLMService;
import com.example.rag.service.RetrievalService;
import com.example.rag.vo.ChunkReference;
import com.example.rag.vo.QueryResponseVO;
import com.example.rag.vo.RetrievalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QueryServiceImpl 单元测试
 * 测试查询服务的核心流程：检索 → 生成 → 返回响应
 */
@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private LLMService llmService;

    @Mock
    private QueryHistoryMapper queryHistoryMapper;

    private RagRetrievalProperties ragRetrievalProperties;

    private QueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        ragRetrievalProperties = new RagRetrievalProperties();
        ragRetrievalProperties.setTopK(5);
        ragRetrievalProperties.setMinScore(0.7);

        queryService = new QueryServiceImpl(
                retrievalService,
                llmService,
                queryHistoryMapper,
                ragRetrievalProperties
        );
    }

    // ==================== query 方法测试 ====================

    /**
     * 测试正常查询流程 - 有检索结果
     */
    @Test
    void testQuery_Success_WithResults() throws Exception {
        // Arrange
        String queryText = "什么是民事权利能力？";
        int topK = 3;

        List<RetrievalResult> retrievalResults = createMockRetrievalResults(2);
        when(retrievalService.retrieve(queryText, topK)).thenReturn(retrievalResults);
        when(llmService.generateAnswer(eq(queryText), anyList()))
                .thenReturn("民事权利能力是指民事主体依法享有民事权利和承担民事义务的资格。");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        QueryResponseVO response = queryService.query(queryText, topK);

        // Assert
        assertNotNull(response);
        assertEquals(queryText, response.getQuery());
        assertEquals("民事权利能力是指民事主体依法享有民事权利和承担民事义务的资格。", response.getAnswer());
        assertNotNull(response.getReferences());
        assertEquals(2, response.getReferences().size());
        assertNotNull(response.getResponseTimeMs());
        assertTrue(response.getResponseTimeMs() >= 0);

        // Verify interactions
        verify(retrievalService).retrieve(queryText, topK);
        verify(llmService).generateAnswer(eq(queryText), anyList());
        verify(queryHistoryMapper).insert(any(QueryHistory.class));
    }

    /**
     * 测试查询 - 无检索结果（仍然调用 LLM）
     */
    @Test
    void testQuery_NoRetrievalResults() throws Exception {
        // Arrange
        String queryText = "一个完全不相关的问题";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), eq(Collections.emptyList())))
                .thenReturn("抱歉，参考资料中没有找到相关信息。");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        QueryResponseVO response = queryService.query(queryText, null);

        // Assert
        assertNotNull(response);
        assertEquals(queryText, response.getQuery());
        assertEquals("抱歉，参考资料中没有找到相关信息。", response.getAnswer());
        assertNotNull(response.getReferences());
        assertTrue(response.getReferences().isEmpty());
        assertNotNull(response.getResponseTimeMs());

        // Verify LLM was still called with empty context
        verify(llmService).generateAnswer(queryText, Collections.emptyList());
    }

    /**
     * 测试查询 - 使用默认 topK（当 topK 为 null）
     */
    @Test
    void testQuery_UsesDefaultTopK_WhenNull() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        queryService.query(queryText, null);

        // Assert - should use default topK of 5
        verify(retrievalService).retrieve(queryText, 5);
    }

    /**
     * 测试查询 - 使用默认 topK（当 topK 为 0 或负数）
     */
    @Test
    void testQuery_UsesDefaultTopK_WhenZeroOrNegative() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        queryService.query(queryText, 0);

        // Assert - should use default topK of 5
        verify(retrievalService).retrieve(queryText, 5);
    }

    /**
     * 测试查询 - 使用自定义 topK
     */
    @Test
    void testQuery_UsesCustomTopK() throws Exception {
        // Arrange
        String queryText = "测试查询";
        int customTopK = 10;
        when(retrievalService.retrieve(queryText, customTopK)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        queryService.query(queryText, customTopK);

        // Assert
        verify(retrievalService).retrieve(queryText, customTopK);
    }

    /**
     * 测试 ChunkReference 构建正确性
     */
    @Test
    void testQuery_ChunkReferencesBuiltCorrectly() throws Exception {
        // Arrange
        String queryText = "测试查询";
        List<RetrievalResult> retrievalResults = new ArrayList<>();

        DocumentChunk chunk1 = createMockChunk(1L, 100L, 0, "第一条内容");
        RetrievalResult result1 = new RetrievalResult(chunk1, 0.95f, "民法典.pdf");
        retrievalResults.add(result1);

        DocumentChunk chunk2 = createMockChunk(2L, 200L, 1, "第二条内容");
        RetrievalResult result2 = new RetrievalResult(chunk2, 0.85f, "合同法.pdf");
        retrievalResults.add(result2);

        when(retrievalService.retrieve(queryText, 5)).thenReturn(retrievalResults);
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        QueryResponseVO response = queryService.query(queryText, null);

        // Assert references
        List<ChunkReference> refs = response.getReferences();
        assertEquals(2, refs.size());

        // First reference
        assertEquals(100L, refs.get(0).getDocumentId());
        assertEquals("民法典.pdf", refs.get(0).getDocumentName());
        assertEquals("第一条内容", refs.get(0).getContent());
        assertEquals(0.95f, refs.get(0).getScore());

        // Second reference
        assertEquals(200L, refs.get(1).getDocumentId());
        assertEquals("合同法.pdf", refs.get(1).getDocumentName());
        assertEquals("第二条内容", refs.get(1).getContent());
        assertEquals(0.85f, refs.get(1).getScore());
    }

    /**
     * 测试查询历史保存 - 正确记录所有字段
     */
    @Test
    void testQuery_SavesQueryHistory() throws Exception {
        // Arrange
        String queryText = "什么是民事权利能力？";
        List<RetrievalResult> retrievalResults = createMockRetrievalResults(1);
        when(retrievalService.retrieve(queryText, 5)).thenReturn(retrievalResults);
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案内容");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        queryService.query(queryText, null);

        // Assert - capture the QueryHistory that was saved
        ArgumentCaptor<QueryHistory> historyCaptor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryMapper).insert(historyCaptor.capture());

        QueryHistory savedHistory = historyCaptor.getValue();
        assertEquals(queryText, savedHistory.getQueryText());
        assertEquals("答案内容", savedHistory.getAnswer());
        assertNotNull(savedHistory.getRetrievedChunks());
        assertNotNull(savedHistory.getQueryTime());
        assertNotNull(savedHistory.getResponseTimeMs());
        assertTrue(savedHistory.getResponseTimeMs() >= 0);

        // Verify retrieved chunks is valid JSON
        assertDoesNotThrow(() -> JSONUtil.parseArray(savedHistory.getRetrievedChunks()));
    }

    /**
     * 测试查询历史保存失败不影响查询结果
     */
    @Test
    void testQuery_HistorySaveFailure_DoesNotAffectResult() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act - should not throw exception
        QueryResponseVO response = queryService.query(queryText, null);

        // Assert - response should still be valid
        assertNotNull(response);
        assertEquals(queryText, response.getQuery());
        assertEquals("答案", response.getAnswer());
    }

    /**
     * 测试检索服务异常传播
     */
    @Test
    void testQuery_RetrievalServiceException_Propagates() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5))
                .thenThrow(new RuntimeException("Retrieval service failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> queryService.query(queryText, null));
    }

    /**
     * 测试 LLM 服务异常传播
     */
    @Test
    void testQuery_LLMServiceException_Propagates() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList()))
                .thenThrow(new RuntimeException("LLM service failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> queryService.query(queryText, null));
    }

    /**
     * 测试响应时间记录
     */
    @Test
    void testQuery_ResponseTimeRecorded() throws Exception {
        // Arrange
        String queryText = "测试查询";
        when(retrievalService.retrieve(queryText, 5)).thenReturn(Collections.emptyList());
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        QueryResponseVO response = queryService.query(queryText, null);

        // Assert
        assertNotNull(response.getResponseTimeMs());
        assertTrue(response.getResponseTimeMs() >= 0, "Response time should be non-negative");
    }

    /**
     * 测试 DocumentChunk 列表正确传递给 LLM 服务
     */
    @Test
    void testQuery_CorrectChunksPassedToLLM() throws Exception {
        // Arrange
        String queryText = "测试查询";
        List<RetrievalResult> retrievalResults = createMockRetrievalResults(3);
        when(retrievalService.retrieve(queryText, 5)).thenReturn(retrievalResults);
        when(llmService.generateAnswer(eq(queryText), anyList())).thenReturn("答案");
        when(queryHistoryMapper.insert(any(QueryHistory.class))).thenReturn(1);

        // Act
        queryService.query(queryText, null);

        // Assert - capture the chunks passed to LLM
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmService).generateAnswer(eq(queryText), chunksCaptor.capture());

        List<DocumentChunk> passedChunks = chunksCaptor.getValue();
        assertEquals(3, passedChunks.size());
        // Verify chunks match the ones from retrieval results
        for (int i = 0; i < passedChunks.size(); i++) {
            assertEquals(retrievalResults.get(i).getChunk(), passedChunks.get(i));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 Mock RetrievalResult 列表
     */
    private List<RetrievalResult> createMockRetrievalResults(int count) {
        List<RetrievalResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DocumentChunk chunk = createMockChunk(
                    (long) (i + 1),
                    (long) ((i % 3) + 1),
                    i,
                    "文档片段内容 " + (i + 1)
            );
            RetrievalResult result = new RetrievalResult(
                    chunk,
                    0.95f - (i * 0.05f),
                    "文档" + ((i % 3) + 1) + ".pdf"
            );
            results.add(result);
        }
        return results;
    }

    /**
     * 创建 Mock DocumentChunk
     */
    private DocumentChunk createMockChunk(Long id, Long documentId, int chunkIndex, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setVectorId("vec-" + id);
        chunk.setCharCount(content.length());
        return chunk;
    }
}
