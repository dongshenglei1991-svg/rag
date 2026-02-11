package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 查询历史实体类
 * 存储用户查询和系统响应的历史记录
 */
@TableName("query_history")
public class QueryHistory {
    
    /**
     * 查询历史ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户查询文本
     */
    private String queryText;
    
    /**
     * 系统生成的答案
     */
    private String answer;
    
    /**
     * 检索到的文档片段信息（JSON格式）
     * 存储片段ID、文档名称、相似度分数等信息
     */
    private String retrievedChunks;
    
    /**
     * 查询时间
     */
    private LocalDateTime queryTime;
    
    /**
     * 响应时间（毫秒）
     */
    private Integer responseTimeMs;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getQueryText() {
        return queryText;
    }
    
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public String getRetrievedChunks() {
        return retrievedChunks;
    }
    
    public void setRetrievedChunks(String retrievedChunks) {
        this.retrievedChunks = retrievedChunks;
    }
    
    public LocalDateTime getQueryTime() {
        return queryTime;
    }
    
    public void setQueryTime(LocalDateTime queryTime) {
        this.queryTime = queryTime;
    }
    
    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}
