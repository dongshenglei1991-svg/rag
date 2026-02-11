package com.example.rag.service;

/**
 * 文档处理服务接口
 * 负责文档的异步处理流程：提取文本 → 分割片段 → 向量化 → 存储向量 → 更新文档状态
 * 
 * 验证需求：2.1, 2.2, 2.3, 2.4, 3.1, 3.4, 10.2
 */
public interface DocumentProcessingService {

    /**
     * 异步处理文档
     * 流程：
     * 1. 从数据库加载文档信息
     * 2. 提取文档文本内容
     * 3. 将文本分割为片段
     * 4. 对每个片段进行向量化
     * 5. 将向量存储到 Qdrant
     * 6. 将片段记录保存到数据库
     * 7. 更新文档状态为 COMPLETED 或 FAILED
     *
     * @param documentId 文档ID
     */
    void processDocument(Long documentId);
}
