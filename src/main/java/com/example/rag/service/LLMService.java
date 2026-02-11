package com.example.rag.service;

import com.example.rag.entity.DocumentChunk;

import java.util.List;

/**
 * 大语言模型服务接口
 * 负责构建提示词和调用 LLM API 生成答案
 */
public interface LLMService {
    
    /**
     * 生成答案（使用默认模型）
     * 
     * @param query 用户查询
     * @param context 检索到的文档片段列表
     * @return 生成的答案
     * @throws Exception 生成失败时抛出异常
     */
    String generateAnswer(String query, List<DocumentChunk> context) throws Exception;
    
    /**
     * 构建提示词
     * 将系统提示、检索内容和用户查询组合成完整的提示词
     * 
     * @param query 用户查询
     * @param context 检索到的文档片段列表
     * @return 构建好的提示词
     */
    String buildPrompt(String query, List<DocumentChunk> context);
    
    /**
     * 生成答案（指定模型）
     * 
     * @param query 用户查询
     * @param context 检索到的文档片段列表
     * @param model 模型名称（如 openai/gpt-4）
     * @return 生成的答案
     * @throws Exception 生成失败时抛出异常
     */
    String generateAnswer(String query, List<DocumentChunk> context, String model) throws Exception;
}
