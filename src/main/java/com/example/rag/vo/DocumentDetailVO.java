package com.example.rag.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档详情视图对象
 * 用于返回给前端的文档详细信息（包含片段列表）
 */
public class DocumentDetailVO {
    
    /**
     * 文档ID
     */
    private Long id;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型（MIME类型）
     */
    private String fileType;
    
    /**
     * 文档处理状态：PROCESSING-处理中, COMPLETED-已完成, FAILED-失败
     */
    private String status;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 处理完成时间
     */
    private LocalDateTime processTime;
    
    /**
     * 文档片段数量
     */
    private Integer chunkCount;
    
    /**
     * 文档片段列表
     */
    private List<ChunkVO> chunks;
    
    // Constructors
    
    public DocumentDetailVO() {
    }
    
    public DocumentDetailVO(Long id, String fileName, Long fileSize, String fileType,
                           String status, LocalDateTime uploadTime, LocalDateTime processTime,
                           Integer chunkCount, List<ChunkVO> chunks) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.status = status;
        this.uploadTime = uploadTime;
        this.processTime = processTime;
        this.chunkCount = chunkCount;
        this.chunks = chunks;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public LocalDateTime getProcessTime() {
        return processTime;
    }
    
    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }
    
    public Integer getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }
    
    public List<ChunkVO> getChunks() {
        return chunks;
    }
    
    public void setChunks(List<ChunkVO> chunks) {
        this.chunks = chunks;
    }
}
