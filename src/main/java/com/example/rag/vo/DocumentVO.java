package com.example.rag.vo;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 * 用于返回给前端的文档信息
 */
public class DocumentVO {
    
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
     * 文档片段数量
     */
    private Integer chunkCount;
    
    // Constructors
    
    public DocumentVO() {
    }
    
    public DocumentVO(Long id, String fileName, Long fileSize, String fileType, 
                     String status, LocalDateTime uploadTime, Integer chunkCount) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.status = status;
        this.uploadTime = uploadTime;
        this.chunkCount = chunkCount;
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
    
    public Integer getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }
}
