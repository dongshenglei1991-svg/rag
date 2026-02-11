package com.example.rag.service;

import com.example.rag.vo.SearchResult;
import com.example.rag.vo.VectorPoint;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务接口
 * 负责向量的存储、检索和删除操作
 * 底层使用 Qdrant 向量数据库
 */
public interface VectorStoreService {

    /**
     * 存储单个向量到 Qdrant
     *
     * @param id       向量点的唯一标识符（UUID 格式）
     * @param vector   向量数据
     * @param metadata 元数据，包含 document_id、chunk_id、chunk_index、content、document_name
     */
    void storeVector(String id, float[] vector, Map<String, Object> metadata);

    /**
     * 批量存储向量到 Qdrant
     *
     * @param points 向量点列表
     */
    void storeVectorBatch(List<VectorPoint> points);

    /**
     * 相似度搜索
     *
     * @param queryVector 查询向量
     * @param topK        返回的最大结果数量
     * @return 搜索结果列表，按相似度分数降序排列
     */
    List<SearchResult> search(float[] queryVector, int topK);

    /**
     * 删除单个向量
     *
     * @param id 向量点的唯一标识符
     */
    void deleteVector(String id);

    /**
     * 删除指定文档的所有向量
     *
     * @param documentId 文档ID
     */
    void deleteByDocumentId(Long documentId);
}
