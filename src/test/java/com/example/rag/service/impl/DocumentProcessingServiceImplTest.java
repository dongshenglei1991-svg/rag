package com.example.rag.service.impl;

import com.example.rag.config.RagDocumentProperties;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentChunkMapper;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.DocumentProcessor;
import com.example.rag.service.EmbeddingService;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.VectorPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocumentProcessingServiceImpl 单元测试
 * 测试异步文档处理流程：提取文本 → 分割片段 → 向量化 → 存储向量 → 更新文档状态
 */
@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceImplTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private RagDocumentProperties ragDocumentProperties;

    @InjectMocks
    private DocumentProcessingServiceImpl documentProcessingService;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Captor
    private ArgumentCaptor<DocumentChunk> chunkCaptor;

    @Captor
    private ArgumentCaptor<List<VectorPoint>> vectorPointsCaptor;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setFileName("test.pdf");
        testDocument.setFileSize(1024L);
        testDocument.setFileType("application/pdf");
        testDocument.setFilePath("/uploads/test.pdf");
        testDocument.setStatus("PROCESSING");
        testDocument.setUploadTime(LocalDateTime.now());
        testDocument.setChunkCount(0);
    }

    @Test
    void testProcessDocument_Success() throws Exception {
        // Arrange
        String extractedText = "This is a test document with enough content to be split into chunks.";
        List<String> chunks = Arrays.asList("chunk1 content", "chunk2 content", "chunk3 content");
        float[] mockVector = new float[]{0.1f, 0.2f, 0.3f};

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(embeddingService.embed(anyString())).thenReturn(mockVector);
        when(documentChunkMapper.insert(any(DocumentChunk.class))).thenReturn(1);
        when(documentMapper.updateById(any(Document.class))).thenReturn(1);

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - verify text extraction
        verify(documentProcessor).extractText(any(File.class));

        // Assert - verify text splitting
        verify(documentProcessor).splitDocument(extractedText, 800, 150);

        // Assert - verify embedding called for each chunk
        verify(embeddingService, times(3)).embed(anyString());
        verify(embeddingService).embed("chunk1 content");
        verify(embeddingService).embed("chunk2 content");
        verify(embeddingService).embed("chunk3 content");

        // Assert - verify vector batch storage
        verify(vectorStoreService).storeVectorBatch(vectorPointsCaptor.capture());
        List<VectorPoint> storedPoints = vectorPointsCaptor.getValue();
        assertEquals(3, storedPoints.size());

        // Verify each vector point has correct metadata
        for (int i = 0; i < storedPoints.size(); i++) {
            VectorPoint point = storedPoints.get(i);
            assertNotNull(point.getId());
            assertArrayEquals(mockVector, point.getVector());
            assertEquals(1L, point.getMetadata().get("document_id"));
            assertEquals(i, point.getMetadata().get("chunk_index"));
            assertEquals(chunks.get(i), point.getMetadata().get("content"));
            assertEquals("test.pdf", point.getMetadata().get("document_name"));
        }

        // Assert - verify chunk records saved to DB
        verify(documentChunkMapper, times(3)).insert(any(DocumentChunk.class));

        // Assert - verify document status updated to COMPLETED
        verify(documentMapper).updateById(documentCaptor.capture());
        Document updatedDocument = documentCaptor.getValue();
        assertEquals("COMPLETED", updatedDocument.getStatus());
        assertEquals(3, updatedDocument.getChunkCount());
        assertNotNull(updatedDocument.getProcessTime());
    }

    @Test
    void testProcessDocument_DocumentNotFound() {
        // Arrange
        when(documentMapper.selectById(999L)).thenReturn(null);

        // Act
        documentProcessingService.processDocument(999L);

        // Assert - no further processing should happen
        verifyNoInteractions(documentProcessor);
        verifyNoInteractions(embeddingService);
        verifyNoInteractions(vectorStoreService);
        verifyNoInteractions(documentChunkMapper);
    }

    @Test
    void testProcessDocument_TextExtractionFails() throws Exception {
        // Arrange
        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class)))
                .thenThrow(new RuntimeException("Failed to extract text"));

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - document status should be FAILED
        verify(documentMapper, times(2)).selectById(1L); // once for processing, once for error handling
        verify(documentMapper).updateById(documentCaptor.capture());
        Document updatedDocument = documentCaptor.getValue();
        assertEquals("FAILED", updatedDocument.getStatus());
        assertEquals("Failed to extract text", updatedDocument.getErrorMessage());
        assertNotNull(updatedDocument.getProcessTime());

        // No embedding or vector storage should happen
        verifyNoInteractions(embeddingService);
        verifyNoInteractions(vectorStoreService);
    }

    @Test
    void testProcessDocument_EmbeddingFails() throws Exception {
        // Arrange
        String extractedText = "Test text content";
        List<String> chunks = Arrays.asList("chunk1", "chunk2");

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(embeddingService.embed("chunk1")).thenReturn(new float[]{0.1f});
        when(embeddingService.embed("chunk2")).thenThrow(new RuntimeException("Embedding API error"));

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - document status should be FAILED
        verify(documentMapper).updateById(documentCaptor.capture());
        Document updatedDocument = documentCaptor.getValue();
        assertEquals("FAILED", updatedDocument.getStatus());
        assertEquals("Embedding API error", updatedDocument.getErrorMessage());

        // Vector storage should not happen since embedding failed
        verifyNoInteractions(vectorStoreService);
    }

    @Test
    void testProcessDocument_VectorStorageFails() throws Exception {
        // Arrange
        String extractedText = "Test text content";
        List<String> chunks = Collections.singletonList("chunk1");
        float[] mockVector = new float[]{0.1f, 0.2f};

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(embeddingService.embed(anyString())).thenReturn(mockVector);
        doThrow(new RuntimeException("Qdrant storage error"))
                .when(vectorStoreService).storeVectorBatch(anyList());

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - document status should be FAILED
        verify(documentMapper).updateById(documentCaptor.capture());
        Document updatedDocument = documentCaptor.getValue();
        assertEquals("FAILED", updatedDocument.getStatus());
        assertEquals("Qdrant storage error", updatedDocument.getErrorMessage());
    }

    @Test
    void testProcessDocument_ChunkRecordsHaveCorrectFields() throws Exception {
        // Arrange
        String extractedText = "Test text";
        List<String> chunks = Arrays.asList("first chunk content", "second chunk content");
        float[] mockVector = new float[]{0.1f};

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(embeddingService.embed(anyString())).thenReturn(mockVector);
        when(documentChunkMapper.insert(any(DocumentChunk.class))).thenReturn(1);
        when(documentMapper.updateById(any(Document.class))).thenReturn(1);

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - verify chunk records
        verify(documentChunkMapper, times(2)).insert(chunkCaptor.capture());
        List<DocumentChunk> savedChunks = chunkCaptor.getAllValues();

        // First chunk
        DocumentChunk chunk0 = savedChunks.get(0);
        assertEquals(1L, chunk0.getDocumentId());
        assertEquals(0, chunk0.getChunkIndex());
        assertEquals("first chunk content", chunk0.getContent());
        assertNotNull(chunk0.getVectorId());
        assertEquals("first chunk content".length(), chunk0.getCharCount());

        // Second chunk
        DocumentChunk chunk1 = savedChunks.get(1);
        assertEquals(1L, chunk1.getDocumentId());
        assertEquals(1, chunk1.getChunkIndex());
        assertEquals("second chunk content", chunk1.getContent());
        assertNotNull(chunk1.getVectorId());
        assertEquals("second chunk content".length(), chunk1.getCharCount());

        // Vector IDs should be unique
        assertNotEquals(chunk0.getVectorId(), chunk1.getVectorId());
    }

    @Test
    void testProcessDocument_UniqueVectorIds() throws Exception {
        // Arrange
        String extractedText = "Test text";
        List<String> chunks = Arrays.asList("chunk1", "chunk2", "chunk3", "chunk4");
        float[] mockVector = new float[]{0.1f};

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(embeddingService.embed(anyString())).thenReturn(mockVector);
        when(documentChunkMapper.insert(any(DocumentChunk.class))).thenReturn(1);
        when(documentMapper.updateById(any(Document.class))).thenReturn(1);

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - all vector IDs should be unique
        verify(vectorStoreService).storeVectorBatch(vectorPointsCaptor.capture());
        List<VectorPoint> points = vectorPointsCaptor.getValue();
        long uniqueIds = points.stream().map(VectorPoint::getId).distinct().count();
        assertEquals(4, uniqueIds, "All vector IDs should be unique");
    }

    @Test
    void testProcessDocument_EmptyChunks() throws Exception {
        // Arrange
        String extractedText = "";
        List<String> chunks = Collections.emptyList();

        when(documentMapper.selectById(1L)).thenReturn(testDocument);
        when(documentProcessor.extractText(any(File.class))).thenReturn(extractedText);
        when(ragDocumentProperties.getChunkSize()).thenReturn(800);
        when(ragDocumentProperties.getChunkOverlap()).thenReturn(150);
        when(documentProcessor.splitDocument(extractedText, 800, 150)).thenReturn(chunks);
        when(documentMapper.updateById(any(Document.class))).thenReturn(1);

        // Act
        documentProcessingService.processDocument(1L);

        // Assert - no embedding or vector storage should happen
        verifyNoInteractions(embeddingService);
        verify(vectorStoreService, never()).storeVectorBatch(anyList());
        verify(documentChunkMapper, never()).insert(any(DocumentChunk.class));

        // Document should still be marked as COMPLETED with 0 chunks
        verify(documentMapper).updateById(documentCaptor.capture());
        Document updatedDocument = documentCaptor.getValue();
        assertEquals("COMPLETED", updatedDocument.getStatus());
        assertEquals(0, updatedDocument.getChunkCount());
    }

    @Test
    void testProcessDocument_FailedStatusUpdateAlsoFails() throws Exception {
        // Arrange - both processing and status update fail
        when(documentMapper.selectById(1L))
                .thenReturn(testDocument)  // first call for processing
                .thenReturn(null);         // second call for error handling (document gone)
        when(documentProcessor.extractText(any(File.class)))
                .thenThrow(new RuntimeException("Processing error"));

        // Act - should not throw even when error handling fails
        assertDoesNotThrow(() -> documentProcessingService.processDocument(1L));

        // Assert - updateById should not be called since document is null in error handler
        verify(documentMapper, never()).updateById(any(Document.class));
    }
}
