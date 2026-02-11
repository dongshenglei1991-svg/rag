package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * RAG 文档配置属性
 * 从 application.yml 中读取 rag.document 配置
 */
@Component
@ConfigurationProperties(prefix = "rag.document")
public class RagDocumentProperties {
    
    /**
     * 文档片段大小（字符数）
     */
    private Integer chunkSize = 800;
    
    /**
     * 片段重叠大小（字符数）
     */
    private Integer chunkOverlap = 150;
    
    /**
     * 支持的文件格式（逗号分隔）
     */
    private String supportedFormats = "pdf,txt,docx,md";
    
    /**
     * 最大文件大小（字节）
     */
    private Long maxFileSize = 52428800L; // 50MB
    
    /**
     * 文件上传目录
     */
    private String uploadDir = "./uploads";
    
    // Getters and Setters
    
    public Integer getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public Integer getChunkOverlap() {
        return chunkOverlap;
    }
    
    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }
    
    public String getSupportedFormats() {
        return supportedFormats;
    }
    
    public void setSupportedFormats(String supportedFormats) {
        this.supportedFormats = supportedFormats;
    }
    
    public Long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public String getUploadDir() {
        return uploadDir;
    }
    
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
    
    /**
     * 获取支持的文件格式列表
     * 
     * @return 支持的文件格式列表
     */
    public List<String> getSupportedFormatsList() {
        return Arrays.asList(supportedFormats.toLowerCase().split(","));
    }
    
    /**
     * 检查文件格式是否支持
     * 
     * @param format 文件格式（如 pdf, txt）
     * @return 是否支持
     */
    public boolean isSupportedFormat(String format) {
        if (format == null) {
            return false;
        }
        return getSupportedFormatsList().contains(format.toLowerCase());
    }
}
