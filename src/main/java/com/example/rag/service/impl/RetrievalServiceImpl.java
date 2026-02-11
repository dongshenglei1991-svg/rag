package com.example.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.rag.config.RagRetrievalProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentChunkMapper;
import com.example.rag.service.EmbeddingService;
import com.example.rag.service.RetrievalService;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.RetrievalResult;
import com.example.rag.vo.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 检索服务实现类
 * 负责将查询向量化，在 Qdrant 中执行相似度搜索，并返回完整的文档片段信息
 *
 * 需求：4.1 - 将查询文本向量化
 * 需求：4.2 - 在向量数据库中执行相似度搜索
 * 需求：4.3 - 返回前 K 个最相关的文档片段（K 默认为 5，可配置）
 * 需求：4.4 - 包含每个片段的相似度分数和原始文本内容
 */
@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalServiceImpl.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentChunkMapper documentChunkMapper;
    private final RagRetrievalProperties ragRetrievalProperties;

    public RetrievalServiceImpl(EmbeddingService embeddingService,
                                VectorStoreService vectorStoreService,
                                DocumentChunkMapper documentChunkMapper,
                                RagRetrievalProperties ragRetrievalProperties) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.documentChunkMapper = documentChunkMapper;
        this.ragRetrievalProperties = ragRetrievalProperties;
    }

    @Override
    public List<RetrievalResult> retrieve(String query, int topK) throws Exception {
        if (query == null || query.isBlank()) {
            log.warn("Empty query received, returning empty results");
            return Collections.emptyList();
        }

        // Use configured topK if the provided value is <= 0
        int effectiveTopK = topK > 0 ? topK : ragRetrievalProperties.getTopK();
        log.info("Retrieving documents for query: '{}', topK={}", query, effectiveTopK);

        // Step 1: Vectorize the query (Requirement 4.1)
        log.debug("Step 1: Vectorizing query text");
        float[] queryVector = embeddingService.embed(query);
        log.debug("Query vectorized successfully, dimension={}", queryVector.length);

        // Step 2: Search in Qdrant (Requirement 4.2)
        log.debug("Step 2: Executing similarity search in vector store");
        List<SearchResult> searchResults = vectorStoreService.search(queryVector, effectiveTopK);

        if (searchResults == null || searchResults.isEmpty()) {
            log.info("No search results found for query: '{}'", query);
            return Collections.emptyList();
        }

        log.debug("Found {} search results from vector store", searchResults.size());

        // Step 3: Look up DocumentChunk from DB for each result
        log.debug("Step 3: Looking up DocumentChunk records from database");
        List<RetrievalResult> retrievalResults = new ArrayList<>();

        for (SearchResult searchResult : searchResults) {
            String vectorId = searchResult.getId();
            Float score = searchResult.getScore();

            // Extract document_name from payload if available
            String documentName = null;
            if (searchResult.getPayload() != null) {
                Object docNameObj = searchResult.getPayload().get("document_name");
                if (docNameObj != null) {
                    documentName = docNameObj.toString();
                }
            }

            // Query DocumentChunk by vector_id
            QueryWrapper<DocumentChunk> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("vector_id", vectorId);
            DocumentChunk chunk = documentChunkMapper.selectOne(queryWrapper);

            if (chunk == null) {
                log.warn("DocumentChunk not found in database for vector_id='{}', skipping", vectorId);
                continue;
            }

            RetrievalResult result = new RetrievalResult(chunk, score, documentName);
            retrievalResults.add(result);
        }

        // Step 4: Sort by score descending (Requirement 4.3)
        retrievalResults.sort(Comparator.comparing(RetrievalResult::getScore, Comparator.reverseOrder()));

        log.info("Retrieved {} document chunks for query: '{}'", retrievalResults.size(), query);
        return retrievalResults;
    }
}
