package com.example.rag.service;

import com.example.rag.vo.RetrievalResult;

import java.util.List;

/**
 * 检索服务接口
 * 负责根据查询检索相关文档片段
 *
 * 需求：4.1, 4.2, 4.3, 4.4
 */
public interface RetrievalService {

    /**
     * 检索相关文档片段
     *
     * @param query 查询文本
     * @param topK  返回的最大结果数量
     * @return 检索结果列表，包含文档片段和相似度分数，按分数降序排列
     * @throws Exception 检索失败时抛出异常
     */
    List<RetrievalResult> retrieve(String query, int topK) throws Exception;
}
