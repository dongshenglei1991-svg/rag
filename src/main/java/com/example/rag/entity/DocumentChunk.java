package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 文档片段实体类
 * 存储文档分割后的片段信息
 */
@TableName("document_chunk")
public class DocumentChunk {
    
    /**
     * 片段ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属文档ID（外键）
     */
    private Long documentId;
    
    /**
     * 片段索引（在文档中的顺序）
     */
    private Integer chunkIndex;
    
    /**
     * 片段文本内容
     */
    private String content;
    
    /**
     * Qdrant向量数据库中的向量ID
     */
    private String vectorId;
    
    /**
     * 片段字符数
     */
    private Integer charCount;
    
    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    
    public Integer getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getVectorId() {
        return vectorId;
    }
    
    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }
    
    public Integer getCharCount() {
        return charCount;
    }
    
    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
