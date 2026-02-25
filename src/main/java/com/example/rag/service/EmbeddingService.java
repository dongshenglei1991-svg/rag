package com.example.rag.service;

import java.util.List;

/**
 * 向量化服务接口
 * 负责将文本转换为向量表示
 */
public interface EmbeddingService {
    
    /**
     * 向量化单个文本
     * 
     * @param text 要向量化的文本
     * @return 向量数组（固定维度，例如 1536 维）
     * @throws Exception 向量化失败时抛出异常
     */
    float[] embed(String text) throws Exception;
    
    /**
     * 批量向量化
     * 
     * @param texts 要向量化的文本列表
     * @return 向量数组列表
     * @throws Exception 向量化失败时抛出异常
     */
    List<float[]> embedBatch(List<String> texts) throws Exception;
    
    /**
     * 获取向量维度
     * 
     * @return 向量维度（例如 1536）
     */
    int getDimension();
}
