package com.example.rag.service;

import java.io.File;
import java.util.List;

/**
 * 文档处理器接口
 * 负责文档的文本提取和分割
 * 
 * 验证需求：2.1
 */
public interface DocumentProcessor {
    
    /**
     * 提取文档的文本内容
     * 支持 PDF、TXT、DOCX、MD 格式
     * 
     * @param file 文档文件
     * @return 提取的文本内容
     * @throws Exception 提取失败时抛出异常
     */
    String extractText(File file) throws Exception;
    
    /**
     * 将文本分割成固定大小的片段
     * 
     * @param text 要分割的文本
     * @param chunkSize 片段大小（字符数）
     * @param overlap 片段之间的重叠大小（字符数）
     * @return 分割后的文本片段列表
     */
    List<String> splitDocument(String text, int chunkSize, int overlap);
    
    /**
     * 检查文件类型是否支持
     * 
     * @param mimeType 文件的 MIME 类型
     * @return 如果支持返回 true，否则返回 false
     */
    boolean isSupported(String mimeType);
}
