package com.example.rag.vo;

import java.time.LocalDateTime;

/**
 * 查询历史数据对象
 * 用于展示查询历史记录
 *
 * 需求：7.4 - 获取查询历史 GET /api/query/history
 */
public class QueryHistoryVO {

    /**
     * 查询历史ID
     */
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
     * 查询时间
     */
    private LocalDateTime queryTime;

    /**
     * 响应时间（毫秒）
     */
    private Integer responseTimeMs;

    // Constructors

    public QueryHistoryVO() {
    }

    public QueryHistoryVO(Long id, String queryText, String answer, LocalDateTime queryTime, Integer responseTimeMs) {
        this.id = id;
        this.queryText = queryText;
        this.answer = answer;
        this.queryTime = queryTime;
        this.responseTimeMs = responseTimeMs;
    }

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
