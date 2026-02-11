package com.example.rag.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.rag.service.DocumentProcessor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 文档处理器实现类
 * 使用 Apache Tika 提取文本内容
 * 
 * 验证需求：2.1
 */
@Service
public class DocumentProcessorImpl implements DocumentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessorImpl.class);
    
    private final Tika tika;
    
    /**
     * 支持的 MIME 类型
     */
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "application/pdf",           // PDF
        "text/plain",                // TXT
        "text/markdown",             // MD
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
        "application/msword"         // DOC (旧版 Word)
    );
    
    public DocumentProcessorImpl() {
        this.tika = new Tika();
    }
    
    /**
     * 提取文档的文本内容
     * 使用 Apache Tika 自动检测文件类型并提取文本
     * 
     * @param file 文档文件
     * @return 提取的文本内容
     * @throws Exception 提取失败时抛出异常
     */
    @Override
    public String extractText(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("不是有效的文件");
        }
        
        try {
            log.debug("开始提取文本，文件: {}", file.getName());
            
            // 使用 Tika 提取文本
            String text = tika.parseToString(file);
            
            // 清理文本（移除多余空白）
            text = StrUtil.cleanBlank(text);
            
            log.debug("文本提取成功，文件: {}, 文本长度: {}", file.getName(), text.length());
            
            return text;
            
        } catch (IOException e) {
            log.error("文件读取失败: {}", file.getName(), e);
            throw new Exception("文件读取失败: " + e.getMessage(), e);
        } catch (TikaException e) {
            log.error("文本提取失败: {}", file.getName(), e);
            throw new Exception("文本提取失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("文档处理失败: {}", file.getName(), e);
            throw new Exception("文档处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将文本分割成固定大小的片段
     * 保留片段之间的重叠以保持上下文连贯性
     * 
     * @param text 要分割的文本
     * @param chunkSize 片段大小（字符数）
     * @param overlap 片段之间的重叠大小（字符数）
     * @return 分割后的文本片段列表
     */
    @Override
    public List<String> splitDocument(String text, int chunkSize, int overlap) {
        if (StrUtil.isBlank(text)) {
            log.debug("文本为空，返回空列表");
            return Collections.emptyList();
        }
        
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("片段大小必须大于 0");
        }
        
        if (overlap < 0) {
            throw new IllegalArgumentException("重叠大小不能为负数");
        }
        
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("重叠大小必须小于片段大小");
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();
        
        log.debug("开始分割文本，文本长度: {}, 片段大小: {}, 重叠: {}", textLength, chunkSize, overlap);
        
        while (start < textLength) {
            // 计算当前片段的结束位置
            int end = Math.min(start + chunkSize, textLength);
            
            // 提取片段
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            
            // 计算下一个片段的起始位置（考虑重叠）
            start += (chunkSize - overlap);
            
            // 如果剩余文本长度小于重叠大小，直接结束
            if (start >= textLength) {
                break;
            }
        }
        
        log.debug("文本分割完成，共 {} 个片段", chunks.size());
        
        return chunks;
    }
    
    /**
     * 检查文件类型是否支持
     * 
     * @param mimeType 文件的 MIME 类型
     * @return 如果支持返回 true，否则返回 false
     */
    @Override
    public boolean isSupported(String mimeType) {
        if (StrUtil.isBlank(mimeType)) {
            return false;
        }
        
        // 检查是否在支持的 MIME 类型列表中
        boolean supported = SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
        
        log.debug("MIME 类型: {}, 是否支持: {}", mimeType, supported);
        
        return supported;
    }
}
