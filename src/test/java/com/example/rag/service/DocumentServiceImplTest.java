package com.example.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.config.QdrantProperties;
import com.example.rag.entity.Document;
import com.example.rag.exception.BusinessException;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.DocumentProcessingService;
import com.example.rag.service.impl.DocumentServiceImpl;
import com.example.rag.vo.DocumentVO;
import com.example.rag.vo.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DocumentServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {
    
    @Mock
    private DocumentMapper documentMapper;
    
    @Mock
    private QdrantClient qdrantClient;
    
    @Mock
    private QdrantProperties qdrantProperties;
    
    @Mock
    private DocumentProcessingService documentProcessingService;
    
    @InjectMocks
    private DocumentServiceImpl documentService;
    
    private Document document1;
    private Document document2;
    private Document document3;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        document1 = createDocument(1L, "test1.pdf", "COMPLETED", 
            LocalDateTime.of(2024, 1, 15, 10, 30));
        document2 = createDocument(2L, "test2.txt", "PROCESSING", 
            LocalDateTime.of(2024, 1, 15, 11, 0));
        document3 = createDocument(3L, "test3.docx", "COMPLETED", 
            LocalDateTime.of(2024, 1, 15, 9, 0));
    }
    
    @Test
    void testListDocuments_Success() {
        // 准备分页数据
        Page<Document> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(document2, document1, document3));
        mockPage.setTotal(3);
        
        // Mock mapper 行为
        when(documentMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
            .thenReturn(mockPage);
        
        // 执行测试
        PageResult<DocumentVO> result = documentService.listDocuments(1, 10);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(3L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(3, result.getRecords().size());
        
        // 验证第一条记录（应该是最新的）
        DocumentVO firstVO = result.getRecords().get(0);
        assertEquals(2L, firstVO.getId());
        assertEquals("test2.txt", firstVO.getFileName());
        assertEquals("PROCESSING", firstVO.getStatus());
        
        // 验证 mapper 被正确调用
        verify(documentMapper, times(1)).selectPage(any(Page.class), any(QueryWrapper.class));
    }
    
    @Test
    void testListDocuments_EmptyResult() {
        // 准备空分页数据
        Page<Document> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0);
        
        // Mock mapper 行为
        when(documentMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
            .thenReturn(mockPage);
        
        // 执行测试
        PageResult<DocumentVO> result = documentService.listDocuments(1, 10);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertTrue(result.getRecords().isEmpty());
        
        // 验证 mapper 被正确调用
        verify(documentMapper, times(1)).selectPage(any(Page.class), any(QueryWrapper.class));
    }
    
    @Test
    void testListDocuments_SecondPage() {
        // 准备第二页数据
        Page<Document> mockPage = new Page<>(2, 2);
        mockPage.setRecords(Collections.singletonList(document3));
        mockPage.setTotal(3);
        
        // Mock mapper 行为
        when(documentMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
            .thenReturn(mockPage);
        
        // 执行测试
        PageResult<DocumentVO> result = documentService.listDocuments(2, 2);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(3L, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(1, result.getRecords().size());
        
        // 验证记录内容
        DocumentVO vo = result.getRecords().get(0);
        assertEquals(3L, vo.getId());
        assertEquals("test3.docx", vo.getFileName());
    }
    
    @Test
    void testListDocuments_OrderByUploadTimeDesc() {
        // 准备数据（按上传时间倒序）
        Page<Document> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(document2, document1, document3));
        mockPage.setTotal(3);
        
        // Mock mapper 行为
        when(documentMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
            .thenReturn(mockPage);
        
        // 执行测试
        PageResult<DocumentVO> result = documentService.listDocuments(1, 10);
        
        // 验证结果按上传时间倒序排列
        List<DocumentVO> records = result.getRecords();
        assertEquals(3, records.size());
        
        // document2 (11:00) 应该在第一位
        assertEquals(2L, records.get(0).getId());
        // document1 (10:30) 应该在第二位
        assertEquals(1L, records.get(1).getId());
        // document3 (09:00) 应该在第三位
        assertEquals(3L, records.get(2).getId());
    }
    
    @Test
    void testListDocuments_VOFieldsMapping() {
        // 准备数据
        Page<Document> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(document1));
        mockPage.setTotal(1);
        
        // Mock mapper 行为
        when(documentMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
            .thenReturn(mockPage);
        
        // 执行测试
        PageResult<DocumentVO> result = documentService.listDocuments(1, 10);
        
        // 验证 VO 字段映射正确
        DocumentVO vo = result.getRecords().get(0);
        assertEquals(document1.getId(), vo.getId());
        assertEquals(document1.getFileName(), vo.getFileName());
        assertEquals(document1.getFileSize(), vo.getFileSize());
        assertEquals(document1.getFileType(), vo.getFileType());
        assertEquals(document1.getStatus(), vo.getStatus());
        assertEquals(document1.getUploadTime(), vo.getUploadTime());
        assertEquals(document1.getChunkCount(), vo.getChunkCount());
    }
    
    /**
     * 创建测试用的 Document 对象
     */
    private Document createDocument(Long id, String fileName, String status, LocalDateTime uploadTime) {
        Document document = new Document();
        document.setId(id);
        document.setFileName(fileName);
        document.setFileSize(1024L * 100); // 100KB
        document.setFileType("application/pdf");
        document.setFilePath("/uploads/" + fileName);
        document.setStatus(status);
        document.setUploadTime(uploadTime);
        document.setChunkCount(10);
        return document;
    }
    
    @Test
    void testDeleteDocument_Success() {
        // 准备测试数据
        Long documentId = 1L;
        Document document = createDocument(documentId, "test.pdf", "COMPLETED", 
            LocalDateTime.now());
        
        // Mock 行为
        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(qdrantProperties.getCollectionName()).thenReturn("document_chunks");
        when(qdrantClient.deletePointsByDocumentId("document_chunks", documentId)).thenReturn(true);
        when(documentMapper.deleteById(documentId)).thenReturn(1);
        
        // 执行测试
        assertDoesNotThrow(() -> documentService.deleteDocument(documentId));
        
        // 验证调用
        verify(documentMapper, times(1)).selectById(documentId);
        verify(qdrantClient, times(1)).deletePointsByDocumentId("document_chunks", documentId);
        verify(documentMapper, times(1)).deleteById(documentId);
    }
    
    @Test
    void testDeleteDocument_DocumentNotFound() {
        // 准备测试数据
        Long documentId = 999L;
        
        // Mock 行为 - 文档不存在
        when(documentMapper.selectById(documentId)).thenReturn(null);
        
        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> documentService.deleteDocument(documentId));
        
        assertEquals(404, exception.getCode());
        assertEquals("文档不存在", exception.getMessage());
        
        // 验证只调用了 selectById，没有调用删除操作
        verify(documentMapper, times(1)).selectById(documentId);
        verify(qdrantClient, never()).deletePointsByDocumentId(anyString(), anyLong());
        verify(documentMapper, never()).deleteById(anyLong());
    }
    
    @Test
    void testDeleteDocument_QdrantDeleteFails() {
        // 准备测试数据
        Long documentId = 1L;
        Document document = createDocument(documentId, "test.pdf", "COMPLETED", 
            LocalDateTime.now());
        
        // Mock 行为 - Qdrant 删除失败但不影响整体流程
        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(qdrantProperties.getCollectionName()).thenReturn("document_chunks");
        when(qdrantClient.deletePointsByDocumentId("document_chunks", documentId)).thenReturn(false);
        when(documentMapper.deleteById(documentId)).thenReturn(1);
        
        // 执行测试 - 应该成功完成，即使 Qdrant 删除失败
        assertDoesNotThrow(() -> documentService.deleteDocument(documentId));
        
        // 验证所有步骤都被调用
        verify(documentMapper, times(1)).selectById(documentId);
        verify(qdrantClient, times(1)).deletePointsByDocumentId("document_chunks", documentId);
        verify(documentMapper, times(1)).deleteById(documentId);
    }
    
    @Test
    void testDeleteDocument_QdrantThrowsException() {
        // 准备测试数据
        Long documentId = 1L;
        Document document = createDocument(documentId, "test.pdf", "COMPLETED", 
            LocalDateTime.now());
        
        // Mock 行为 - Qdrant 抛出异常但不影响整体流程
        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(qdrantProperties.getCollectionName()).thenReturn("document_chunks");
        when(qdrantClient.deletePointsByDocumentId("document_chunks", documentId))
            .thenThrow(new RuntimeException("Qdrant connection error"));
        when(documentMapper.deleteById(documentId)).thenReturn(1);
        
        // 执行测试 - 应该成功完成，即使 Qdrant 抛出异常
        assertDoesNotThrow(() -> documentService.deleteDocument(documentId));
        
        // 验证数据库删除仍然被调用
        verify(documentMapper, times(1)).selectById(documentId);
        verify(documentMapper, times(1)).deleteById(documentId);
    }
    
    @Test
    void testDeleteDocument_DatabaseDeleteFails() {
        // 准备测试数据
        Long documentId = 1L;
        Document document = createDocument(documentId, "test.pdf", "COMPLETED", 
            LocalDateTime.now());
        
        // Mock 行为 - 数据库删除失败
        when(documentMapper.selectById(documentId)).thenReturn(document);
        when(qdrantProperties.getCollectionName()).thenReturn("document_chunks");
        when(qdrantClient.deletePointsByDocumentId("document_chunks", documentId)).thenReturn(true);
        when(documentMapper.deleteById(documentId)).thenReturn(0); // 删除失败
        
        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> documentService.deleteDocument(documentId));
        
        assertEquals(500, exception.getCode());
        assertEquals("文档记录删除失败", exception.getMessage());
        
        // 验证所有步骤都被调用
        verify(documentMapper, times(1)).selectById(documentId);
        verify(qdrantClient, times(1)).deletePointsByDocumentId("document_chunks", documentId);
        verify(documentMapper, times(1)).deleteById(documentId);
    }
}
