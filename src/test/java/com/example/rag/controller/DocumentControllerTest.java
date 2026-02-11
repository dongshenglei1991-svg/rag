package com.example.rag.controller;

import com.example.rag.service.DocumentService;
import com.example.rag.vo.ChunkVO;
import com.example.rag.vo.DocumentDetailVO;
import com.example.rag.vo.DocumentVO;
import com.example.rag.vo.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentController 单元测试
 * 测试文档管理API接口
 */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DocumentService documentService;
    
    private DocumentVO mockDocumentVO;
    private DocumentDetailVO mockDocumentDetailVO;
    private PageResult<DocumentVO> mockPageResult;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        mockDocumentVO = new DocumentVO();
        mockDocumentVO.setId(1L);
        mockDocumentVO.setFileName("test.pdf");
        mockDocumentVO.setFileSize(1024L);
        mockDocumentVO.setFileType("application/pdf");
        mockDocumentVO.setStatus("PROCESSING");
        mockDocumentVO.setUploadTime(LocalDateTime.now());
        mockDocumentVO.setChunkCount(0);
        
        // 准备文档详情数据
        mockDocumentDetailVO = new DocumentDetailVO();
        mockDocumentDetailVO.setId(1L);
        mockDocumentDetailVO.setFileName("test.pdf");
        mockDocumentDetailVO.setFileSize(1024L);
        mockDocumentDetailVO.setFileType("application/pdf");
        mockDocumentDetailVO.setStatus("COMPLETED");
        mockDocumentDetailVO.setUploadTime(LocalDateTime.now());
        mockDocumentDetailVO.setProcessTime(LocalDateTime.now());
        mockDocumentDetailVO.setChunkCount(2);
        
        List<ChunkVO> chunks = new ArrayList<>();
        ChunkVO chunk1 = new ChunkVO(1L, 0, "This is chunk 1", 15);
        ChunkVO chunk2 = new ChunkVO(2L, 1, "This is chunk 2", 15);
        chunks.add(chunk1);
        chunks.add(chunk2);
        mockDocumentDetailVO.setChunks(chunks);
        
        // 准备分页结果数据
        mockPageResult = new PageResult<>();
        mockPageResult.setTotal(1L);
        mockPageResult.setPage(1);
        mockPageResult.setSize(10);
        List<DocumentVO> records = new ArrayList<>();
        records.add(mockDocumentVO);
        mockPageResult.setRecords(records);
    }
    
    /**
     * 测试上传文档 - 成功场景
     */
    @Test
    void testUploadDocument_Success() throws Exception {
        // 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        
        // Mock service 行为
        when(documentService.uploadDocument(any())).thenReturn(mockDocumentVO);
        
        // 执行请求并验证响应
        mockMvc.perform(multipart("/api/documents")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }
    
    /**
     * 测试获取文档列表 - 成功场景
     */
    @Test
    void testListDocuments_Success() throws Exception {
        // Mock service 行为
        when(documentService.listDocuments(1, 10)).thenReturn(mockPageResult);
        
        // 执行请求并验证响应
        mockMvc.perform(get("/api/documents")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].fileName").value("test.pdf"));
    }
    
    /**
     * 测试获取文档列表 - 使用默认参数
     */
    @Test
    void testListDocuments_DefaultParams() throws Exception {
        // Mock service 行为
        when(documentService.listDocuments(1, 10)).thenReturn(mockPageResult);
        
        // 执行请求并验证响应（不传参数，使用默认值）
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }
    
    /**
     * 测试获取文档列表 - 无效的页码参数
     */
    @Test
    void testListDocuments_InvalidPage() throws Exception {
        // 执行请求并验证响应
        mockMvc.perform(get("/api/documents")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("页码必须大于0"));
    }
    
    /**
     * 测试获取文档列表 - 无效的每页大小参数
     */
    @Test
    void testListDocuments_InvalidSize() throws Exception {
        // 执行请求并验证响应
        mockMvc.perform(get("/api/documents")
                        .param("page", "1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("每页大小必须在1-100之间"));
    }
    
    /**
     * 测试删除文档 - 成功场景
     */
    @Test
    void testDeleteDocument_Success() throws Exception {
        // Mock service 行为
        doNothing().when(documentService).deleteDocument(1L);
        
        // 执行请求并验证响应
        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }
    
    /**
     * 测试获取文档详情 - 成功场景
     */
    @Test
    void testGetDocument_Success() throws Exception {
        // Mock service 行为
        when(documentService.getDocument(1L)).thenReturn(mockDocumentDetailVO);
        
        // 执行请求并验证响应
        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.chunkCount").value(2))
                .andExpect(jsonPath("$.data.chunks[0].id").value(1))
                .andExpect(jsonPath("$.data.chunks[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data.chunks[0].content").value("This is chunk 1"))
                .andExpect(jsonPath("$.data.chunks[1].id").value(2))
                .andExpect(jsonPath("$.data.chunks[1].chunkIndex").value(1));
    }
}
