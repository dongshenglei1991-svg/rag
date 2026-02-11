package com.example.rag.vo;

/**
 * 文档片段引用数据对象
 * 用于在查询响应中展示引用的文档片段信息
 *
 * 需求：5.3 - 将答案和引用的文档片段一起返回给用户
 */
public class ChunkReference {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 片段文本内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private Float score;

    // Constructors

    public ChunkReference() {
    }

    public ChunkReference(Long documentId, String documentName, String content, Float score) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.content = content;
        this.score = score;
    }

    // Getters and Setters

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}
