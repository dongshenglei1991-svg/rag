package com.example.rag.vo;

import java.util.Map;

/**
 * 向量搜索结果数据对象
 * 封装从 Qdrant 返回的搜索结果
 */
public class SearchResult {

    /**
     * 向量点的唯一标识符
     */
    private String id;

    /**
     * 相似度分数
     */
    private Float score;

    /**
     * 元数据（payload），包含 document_id、chunk_id、chunk_index、content、document_name
     */
    private Map<String, Object> payload;

    // Constructors

    public SearchResult() {
    }

    public SearchResult(String id, Float score, Map<String, Object> payload) {
        this.id = id;
        this.score = score;
        this.payload = payload;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
