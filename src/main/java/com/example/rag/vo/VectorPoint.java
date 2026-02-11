package com.example.rag.vo;

import java.util.Map;

/**
 * 向量点数据对象
 * 用于封装存储到 Qdrant 的向量数据
 */
public class VectorPoint {

    /**
     * 向量点的唯一标识符（UUID 格式）
     */
    private String id;

    /**
     * 向量数据
     */
    private float[] vector;

    /**
     * 元数据（payload），包含 document_id、chunk_id、chunk_index、content、document_name
     */
    private Map<String, Object> metadata;

    // Constructors

    public VectorPoint() {
    }

    public VectorPoint(String id, float[] vector, Map<String, Object> metadata) {
        this.id = id;
        this.vector = vector;
        this.metadata = metadata;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
