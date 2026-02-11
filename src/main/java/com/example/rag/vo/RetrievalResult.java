package com.example.rag.vo;

import com.example.rag.entity.DocumentChunk;

/**
 * 检索结果数据对象
 * 封装文档片段和相似度分数
 *
 * 需求：4.4 - 检索结果应包含每个片段的相似度分数和原始文本内容
 */
public class RetrievalResult {

    /**
     * 文档片段
     */
    private DocumentChunk chunk;

    /**
     * 相似度分数
     */
    private Float score;

    /**
     * 文档名称
     */
    private String documentName;

    // Constructors

    public RetrievalResult() {
    }

    public RetrievalResult(DocumentChunk chunk, Float score, String documentName) {
        this.chunk = chunk;
        this.score = score;
        this.documentName = documentName;
    }

    // Getters and Setters

    public DocumentChunk getChunk() {
        return chunk;
    }

    public void setChunk(DocumentChunk chunk) {
        this.chunk = chunk;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}
