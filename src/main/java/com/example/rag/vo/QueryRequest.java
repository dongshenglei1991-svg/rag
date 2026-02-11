package com.example.rag.vo;

/**
 * 查询请求数据对象
 * 封装用户提交的查询参数
 *
 * 需求：7.4 - 提交查询 POST /api/query
 */
public class QueryRequest {

    /**
     * 查询文本（必填）
     */
    private String query;

    /**
     * 返回的最大检索结果数量（可选，默认使用配置值）
     */
    private Integer topK;

    // Constructors

    public QueryRequest() {
    }

    public QueryRequest(String query, Integer topK) {
        this.query = query;
        this.topK = topK;
    }

    // Getters and Setters

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
