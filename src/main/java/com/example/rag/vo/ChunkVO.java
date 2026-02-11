package com.example.rag.vo;

/**
 * 文档片段视图对象
 * 用于返回给前端的片段信息
 */
public class ChunkVO {
    
    /**
     * 片段ID
     */
    private Long id;
    
    /**
     * 片段索引
     */
    private Integer chunkIndex;
    
    /**
     * 片段内容
     */
    private String content;
    
    /**
     * 字符数
     */
    private Integer charCount;
    
    // Constructors
    
    public ChunkVO() {
    }
    
    public ChunkVO(Long id, Integer chunkIndex, String content, Integer charCount) {
        this.id = id;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.charCount = charCount;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Integer getCharCount() {
        return charCount;
    }
    
    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }
}
