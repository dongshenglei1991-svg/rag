package com.example.rag.service.impl;

import com.example.rag.config.OpenRouterProperties;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量化服务实现类
 * 使用 OpenRouter API 调用 Embedding 模型
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);
    
    private final WebClient webClient;
    private final OpenRouterProperties properties;
    private final Retry retrySpec;
    
    // Qwen3-Embedding-0.6B 的向量维度
    private static final int EMBEDDING_DIMENSION = 1024;
    
    public EmbeddingServiceImpl(WebClient openRouterWebClient, 
                                OpenRouterProperties properties) {
        this.webClient = openRouterWebClient;
        this.properties = properties;
        
        // 配置重试策略：最多 3 次，指数退避
        this.retrySpec = Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying embedding API call, attempt: {}, error: {}", 
                            retrySignal.totalRetries() + 1, 
                            retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Embedding API retry exhausted after {} attempts", 
                            retrySignal.totalRetries());
                    return new BusinessException(
                            HttpStatus.BAD_GATEWAY.value(),
                            "向量化服务调用失败，已重试 " + retrySignal.totalRetries() + " 次",
                            HttpStatus.BAD_GATEWAY,
                            retrySignal.failure()
                    );
                });
    }
    
    /**
     * 向量化单个文本
     * 
     * @param text 要向量化的文本
     * @return 向量数组
     * @throws Exception 向量化失败时抛出异常
     */
    @Override
    public float[] embed(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "文本内容不能为空");
        }
        
        log.debug("Embedding text, length: {}", text.length());
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getEmbeddingModel());
            requestBody.put("input", text);
            
            // 调用 OpenRouter Embeddings API
            EmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(retrySpec)
                    .block();
            
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.error("Embedding API returned empty data for model: {}", properties.getEmbeddingModel());
                throw new BusinessException(HttpStatus.BAD_GATEWAY.value(), "向量化服务返回空结果");
            }
            
            float[] embedding = response.getData().get(0).getEmbedding();
            
            if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY.value(), 
                        "向量维度不正确，期望: " + EMBEDDING_DIMENSION + ", 实际: " + 
                        (embedding == null ? "null" : embedding.length)
                );
            }
            
            log.debug("Embedding successful, dimension: {}", embedding.length);
            return embedding;
            
        } catch (WebClientResponseException e) {
            log.error("Embedding API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "向量化服务调用失败: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY,
                    e
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during embedding", e);
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量化过程发生错误: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }
    
    /**
     * 批量向量化
     * 
     * @param texts 要向量化的文本列表
     * @return 向量数组列表
     * @throws Exception 向量化失败时抛出异常
     */
    @Override
    public List<float[]> embedBatch(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "文本列表不能为空");
        }
        
        log.debug("Batch embedding {} texts", texts.size());
        
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getEmbeddingModel());
            requestBody.put("input", texts);
            
            // 调用 OpenRouter Embeddings API
            EmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(retrySpec)
                    .block();
            
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY.value(), "向量化服务返回空结果");
            }
            
            if (response.getData().size() != texts.size()) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY.value(),
                        "向量化结果数量不匹配，期望: " + texts.size() + ", 实际: " + response.getData().size()
                );
            }
            
            List<float[]> embeddings = new ArrayList<>();
            for (EmbeddingData data : response.getData()) {
                float[] embedding = data.getEmbedding();
                
                if (embedding == null || embedding.length != EMBEDDING_DIMENSION) {
                    throw new BusinessException(
                            HttpStatus.BAD_GATEWAY.value(),
                            "向量维度不正确，期望: " + EMBEDDING_DIMENSION + ", 实际: " + 
                            (embedding == null ? "null" : embedding.length)
                    );
                }
                
                embeddings.add(embedding);
            }
            
            log.debug("Batch embedding successful, count: {}", embeddings.size());
            return embeddings;
            
        } catch (WebClientResponseException e) {
            log.error("Batch embedding API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "批量向量化服务调用失败: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY,
                    e
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during batch embedding", e);
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "批量向量化过程发生错误: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }
    
    /**
     * 获取向量维度
     * 
     * @return 向量维度
     */
    @Override
    public int getDimension() {
        return EMBEDDING_DIMENSION;
    }
    
    /**
     * Embeddings API 响应结构（OpenAI 兼容格式）
     */
    private static class EmbeddingResponse {
        private String object;
        private List<EmbeddingData> data;
        private String model;
        private Usage usage;
        
        public String getObject() {
            return object;
        }
        
        public void setObject(String object) {
            this.object = object;
        }
        
        public List<EmbeddingData> getData() {
            return data;
        }
        
        public void setData(List<EmbeddingData> data) {
            this.data = data;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public Usage getUsage() {
            return usage;
        }
        
        public void setUsage(Usage usage) {
            this.usage = usage;
        }
    }
    
    /**
     * 单个向量数据
     */
    private static class EmbeddingData {
        private String object;
        private float[] embedding;
        private int index;
        
        public String getObject() {
            return object;
        }
        
        public void setObject(String object) {
            this.object = object;
        }
        
        public float[] getEmbedding() {
            return embedding;
        }
        
        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }
        
        public int getIndex() {
            return index;
        }
        
        public void setIndex(int index) {
            this.index = index;
        }
    }
    
    /**
     * API 使用统计
     */
    private static class Usage {
        private int promptTokens;
        private int totalTokens;
        
        public int getPromptTokens() {
            return promptTokens;
        }
        
        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
