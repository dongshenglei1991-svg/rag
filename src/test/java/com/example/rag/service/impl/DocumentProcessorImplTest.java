package com.example.rag.service.impl;

import com.example.rag.service.DocumentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentProcessor 单元测试
 * 验证需求：2.1
 */
class DocumentProcessorImplTest {
    
    private DocumentProcessor documentProcessor;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        documentProcessor = new DocumentProcessorImpl();
    }
    
    /**
     * 测试提取 TXT 文件文本
     */
    @Test
    void testExtractText_TxtFile_Success() throws Exception {
        // 创建测试文件
        File testFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("这是一个测试文本文件。\n");
            writer.write("包含多行内容。\n");
            writer.write("用于测试文本提取功能。");
        }
        
        // 提取文本
        String text = documentProcessor.extractText(testFile);
        
        // 验证
        assertNotNull(text);
        assertTrue(text.contains("测试文本文件"));
        assertTrue(text.contains("多行内容"));
        assertTrue(text.contains("文本提取功能"));
    }
    
    /**
     * 测试提取空文件
     * Apache Tika 会对空文件抛出异常
     */
    @Test
    void testExtractText_EmptyFile_ThrowsException() throws IOException {
        // 创建空文件
        File emptyFile = tempDir.resolve("empty.txt").toFile();
        emptyFile.createNewFile();
        
        // 验证抛出异常
        assertThrows(Exception.class, () -> {
            documentProcessor.extractText(emptyFile);
        });
    }
    
    /**
     * 测试提取不存在的文件
     */
    @Test
    void testExtractText_NonExistentFile_ThrowsException() {
        File nonExistentFile = new File("non_existent_file.txt");
        
        // 验证抛出异常
        assertThrows(Exception.class, () -> {
            documentProcessor.extractText(nonExistentFile);
        });
    }
    
    /**
     * 测试提取 null 文件
     */
    @Test
    void testExtractText_NullFile_ThrowsException() {
        assertThrows(Exception.class, () -> {
            documentProcessor.extractText(null);
        });
    }
    
    /**
     * 测试正常分割文本
     */
    @Test
    void testSplitDocument_NormalText_Success() {
        String text = "这是一个测试文本。".repeat(100); // 创建一个较长的文本
        int chunkSize = 100;
        int overlap = 20;
        
        List<String> chunks = documentProcessor.splitDocument(text, chunkSize, overlap);
        
        // 验证
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // 验证每个片段（除最后一个）的大小
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertEquals(chunkSize, chunks.get(i).length());
        }
        
        // 验证最后一个片段的大小不超过 chunkSize
        assertTrue(chunks.get(chunks.size() - 1).length() <= chunkSize);
    }
    
    /**
     * 测试分割短文本
     */
    @Test
    void testSplitDocument_ShortText_ReturnsSingleChunk() {
        String text = "这是一个短文本";
        int chunkSize = 100;
        int overlap = 20;
        
        List<String> chunks = documentProcessor.splitDocument(text, chunkSize, overlap);
        
        // 验证
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }
    
    /**
     * 测试分割空文本
     */
    @Test
    void testSplitDocument_EmptyText_ReturnsEmptyList() {
        String text = "";
        int chunkSize = 100;
        int overlap = 20;
        
        List<String> chunks = documentProcessor.splitDocument(text, chunkSize, overlap);
        
        // 验证
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }
    
    /**
     * 测试分割 null 文本
     */
    @Test
    void testSplitDocument_NullText_ReturnsEmptyList() {
        List<String> chunks = documentProcessor.splitDocument(null, 100, 20);
        
        // 验证
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }
    
    /**
     * 测试分割时片段大小为 0
     */
    @Test
    void testSplitDocument_ZeroChunkSize_ThrowsException() {
        String text = "测试文本";
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessor.splitDocument(text, 0, 20);
        });
    }
    
    /**
     * 测试分割时重叠大小为负数
     */
    @Test
    void testSplitDocument_NegativeOverlap_ThrowsException() {
        String text = "测试文本";
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessor.splitDocument(text, 100, -10);
        });
    }
    
    /**
     * 测试分割时重叠大小大于等于片段大小
     */
    @Test
    void testSplitDocument_OverlapGreaterThanChunkSize_ThrowsException() {
        String text = "测试文本";
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessor.splitDocument(text, 100, 100);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            documentProcessor.splitDocument(text, 100, 150);
        });
    }
    
    /**
     * 测试验证片段重叠
     */
    @Test
    void testSplitDocument_VerifyOverlap() {
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int chunkSize = 10;
        int overlap = 3;
        
        List<String> chunks = documentProcessor.splitDocument(text, chunkSize, overlap);
        
        // 验证至少有 2 个片段
        assertTrue(chunks.size() >= 2);
        
        // 验证相邻片段之间有重叠
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentChunk = chunks.get(i);
            String nextChunk = chunks.get(i + 1);
            
            // 当前片段的结尾应该与下一个片段的开头有重叠
            String currentEnd = currentChunk.substring(Math.max(0, currentChunk.length() - overlap));
            String nextStart = nextChunk.substring(0, Math.min(overlap, nextChunk.length()));
            
            // 验证重叠部分相同
            assertEquals(currentEnd, nextStart);
        }
    }
    
    /**
     * 测试支持的 MIME 类型 - PDF
     */
    @Test
    void testIsSupported_Pdf_ReturnsTrue() {
        assertTrue(documentProcessor.isSupported("application/pdf"));
    }
    
    /**
     * 测试支持的 MIME 类型 - TXT
     */
    @Test
    void testIsSupported_Txt_ReturnsTrue() {
        assertTrue(documentProcessor.isSupported("text/plain"));
    }
    
    /**
     * 测试支持的 MIME 类型 - MD
     */
    @Test
    void testIsSupported_Markdown_ReturnsTrue() {
        assertTrue(documentProcessor.isSupported("text/markdown"));
    }
    
    /**
     * 测试支持的 MIME 类型 - DOCX
     */
    @Test
    void testIsSupported_Docx_ReturnsTrue() {
        assertTrue(documentProcessor.isSupported("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }
    
    /**
     * 测试不支持的 MIME 类型
     */
    @Test
    void testIsSupported_UnsupportedType_ReturnsFalse() {
        assertFalse(documentProcessor.isSupported("application/zip"));
        assertFalse(documentProcessor.isSupported("image/jpeg"));
        assertFalse(documentProcessor.isSupported("video/mp4"));
    }
    
    /**
     * 测试空 MIME 类型
     */
    @Test
    void testIsSupported_EmptyMimeType_ReturnsFalse() {
        assertFalse(documentProcessor.isSupported(""));
        assertFalse(documentProcessor.isSupported(null));
    }
    
    /**
     * 测试 MIME 类型大小写不敏感
     */
    @Test
    void testIsSupported_CaseInsensitive() {
        assertTrue(documentProcessor.isSupported("APPLICATION/PDF"));
        assertTrue(documentProcessor.isSupported("Text/Plain"));
        assertTrue(documentProcessor.isSupported("TEXT/MARKDOWN"));
    }
}
