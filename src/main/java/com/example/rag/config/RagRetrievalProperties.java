package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 检索配置属性
 * 从 application.yml 中读取 rag.retrieval 配置
 */
@Component
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    /**
     * 检索返回的文档片段数量（默认 5）
     */
    private Integer topK = 5;

    /**
     * 最小相似度分数阈值（默认 0.7）
     */
    private Double minScore = 0.7;

    // Getters and Setters

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }
}
