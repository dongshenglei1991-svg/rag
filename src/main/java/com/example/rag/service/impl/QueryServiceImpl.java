package com.example.rag.service.impl;

import cn.hutool.json.JSONUtil;
import com.example.rag.config.RagRetrievalProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.entity.QueryHistory;
import com.example.rag.mapper.QueryHistoryMapper;
import com.example.rag.service.LLMService;
import com.example.rag.service.QueryService;
import com.example.rag.service.RetrievalService;
import com.example.rag.vo.ChunkReference;
import com.example.rag.vo.QueryResponseVO;
import com.example.rag.vo.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询服务实现类
 * 协调检索服务和 LLM 服务，完成查询问答流程
 *
 * 需求：4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3
 */
@Service
public class QueryServiceImpl implements QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryServiceImpl.class);

    private final RetrievalService retrievalService;
    private final LLMService llmService;
    private final QueryHistoryMapper queryHistoryMapper;
    private final RagRetrievalProperties ragRetrievalProperties;

    public QueryServiceImpl(RetrievalService retrievalService,
                            LLMService llmService,
                            QueryHistoryMapper queryHistoryMapper,
                            RagRetrievalProperties ragRetrievalProperties) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.queryHistoryMapper = queryHistoryMapper;
        this.ragRetrievalProperties = ragRetrievalProperties;
    }

    @Override
    public QueryResponseVO query(String queryText, Integer topK) throws Exception {
        log.info("Processing query: {}", queryText);

        // 1. Record start time
        long startTime = System.currentTimeMillis();

        // 2. Determine topK value (use config default if not specified)
        int effectiveTopK = (topK != null && topK > 0) ? topK : ragRetrievalProperties.getTopK();
        log.debug("Using topK: {}", effectiveTopK);

        // 3. Retrieve relevant chunks
        List<RetrievalResult> retrievalResults = retrievalService.retrieve(queryText, effectiveTopK);
        log.info("Retrieved {} relevant chunks for query", retrievalResults.size());

        // 4. Extract DocumentChunk list from RetrievalResult list
        List<DocumentChunk> chunks = retrievalResults.stream()
                .map(RetrievalResult::getChunk)
                .collect(Collectors.toList());

        // 5. Generate answer using LLM service
        String answer = llmService.generateAnswer(queryText, chunks);
        log.info("Answer generated successfully");

        // 6. Build ChunkReference list from RetrievalResult list
        List<ChunkReference> references = buildChunkReferences(retrievalResults);

        // 7. Calculate response time
        long responseTimeMs = System.currentTimeMillis() - startTime;
        log.info("Query processed in {} ms", responseTimeMs);

        // 8. Save query history to DB (optional, wrap in try-catch)
        saveQueryHistory(queryText, answer, references, (int) responseTimeMs);

        // 9. Build and return QueryResponseVO
        QueryResponseVO response = new QueryResponseVO();
        response.setQuery(queryText);
        response.setAnswer(answer);
        response.setReferences(references);
        response.setResponseTimeMs(responseTimeMs);

        return response;
    }

    /**
     * Build ChunkReference list from RetrievalResult list
     *
     * @param retrievalResults retrieval results
     * @return list of chunk references
     */
    private List<ChunkReference> buildChunkReferences(List<RetrievalResult> retrievalResults) {
        if (retrievalResults == null || retrievalResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChunkReference> references = new ArrayList<>();
        for (RetrievalResult result : retrievalResults) {
            ChunkReference ref = new ChunkReference();
            DocumentChunk chunk = result.getChunk();
            if (chunk != null) {
                ref.setDocumentId(chunk.getDocumentId());
                ref.setContent(chunk.getContent());
            }
            ref.setDocumentName(result.getDocumentName());
            ref.setScore(result.getScore());
            references.add(ref);
        }
        return references;
    }

    /**
     * Save query history to database (optional, errors are logged but not thrown)
     *
     * @param queryText     user query text
     * @param answer        generated answer
     * @param references    chunk references
     * @param responseTimeMs response time in milliseconds
     */
    private void saveQueryHistory(String queryText, String answer,
                                  List<ChunkReference> references, int responseTimeMs) {
        try {
            QueryHistory history = new QueryHistory();
            history.setQueryText(queryText);
            history.setAnswer(answer);
            history.setRetrievedChunks(JSONUtil.toJsonStr(references));
            history.setQueryTime(LocalDateTime.now());
            history.setResponseTimeMs(responseTimeMs);

            queryHistoryMapper.insert(history);
            log.debug("Query history saved successfully");
        } catch (Exception e) {
            log.warn("Failed to save query history: {}", e.getMessage(), e);
        }
    }
}
