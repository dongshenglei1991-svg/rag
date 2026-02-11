package com.example.rag.service;

import com.example.rag.vo.QueryResponseVO;

/**
 * 查询服务接口
 * 负责处理用户查询请求，协调检索和生成流程
 *
 * 需求：4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3
 */
public interface QueryService {

    /**
     * 处理用户查询
     * 流程：检索相关片段 → 构建提示词 → 生成答案
     *
     * @param queryText 用户查询文本
     * @param topK      返回的最大检索结果数量，为 null 时使用配置默认值
     * @return 查询响应，包含答案和引用的文档片段
     * @throws Exception 查询处理失败时抛出异常
     */
    QueryResponseVO query(String queryText, Integer topK) throws Exception;
}
