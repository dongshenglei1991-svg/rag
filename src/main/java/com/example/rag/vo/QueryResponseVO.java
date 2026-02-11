package com.example.rag.vo;

import java.util.List;

/**
 * 查询响应数据对象
 * 封装查询结果，包含答案和引用的文档片段
 *
 * 需求：4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3
 */
public class QueryResponseVO {

    /**
     * 用户查询文本
     */
    private String query;

    /**
     * LLM 生成的答案
     */
    private String answer;

    /**
     * 引用的文档片段列表
     */
    private List<ChunkReference> references;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;

    // Constructors

    public QueryResponseVO() {
    }

    public QueryResponseVO(String query, String answer, List<ChunkReference> references, Long responseTimeMs) {
        this.query = query;
        this.answer = answer;
        this.references = references;
        this.responseTimeMs = responseTimeMs;
    }

    // Getters and Setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ChunkReference> getReferences() {
        return references;
    }

    public void setReferences(List<ChunkReference> references) {
        this.references = references;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}
