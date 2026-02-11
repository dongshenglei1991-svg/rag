package com.example.rag.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.rag.config.QdrantProperties;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.SearchResult;
import com.example.rag.vo.VectorPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * 向量存储服务实现类
 * 使用 Qdrant REST API 实现向量的存储、检索和删除
 *
 * 需求：3.4 - 将向量和对应的文档片段文本存储到向量数据库
 */
@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreServiceImpl.class);

    private final WebClient qdrantWebClient;
    private final QdrantProperties qdrantProperties;

    public VectorStoreServiceImpl(@Qualifier("qdrantWebClient") WebClient qdrantWebClient,
                                  QdrantProperties qdrantProperties) {
        this.qdrantWebClient = qdrantWebClient;
        this.qdrantProperties = qdrantProperties;
    }

    @Override
    public void storeVector(String id, float[] vector, Map<String, Object> metadata) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "向量ID不能为空");
        }
        if (vector == null || vector.length == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "向量数据不能为空");
        }

        VectorPoint point = new VectorPoint(id, vector, metadata);
        storeVectorBatch(List.of(point));
    }

    @Override
    public void storeVectorBatch(List<VectorPoint> points) {
        if (points == null || points.isEmpty()) {
            log.warn("No vector points to store, skipping");
            return;
        }

        String collectionName = qdrantProperties.getCollectionName();
        log.info("Storing {} vector points to collection '{}'", points.size(), collectionName);

        try {
            // Build the request body for Qdrant upsert
            // PUT /collections/{collection_name}/points
            // Body: {"points": [{"id": "uuid", "vector": [...], "payload": {...}}]}
            JSONObject requestBody = new JSONObject();
            JSONArray pointsArray = new JSONArray();

            for (VectorPoint point : points) {
                JSONObject pointObj = new JSONObject();
                pointObj.set("id", point.getId());
                pointObj.set("vector", point.getVector());

                if (point.getMetadata() != null) {
                    JSONObject payload = new JSONObject();
                    for (Map.Entry<String, Object> entry : point.getMetadata().entrySet()) {
                        payload.set(entry.getKey(), entry.getValue());
                    }
                    pointObj.set("payload", payload);
                }

                pointsArray.add(pointObj);
            }

            requestBody.set("points", pointsArray);

            String requestJson = requestBody.toString();
            log.debug("Upsert request to collection '{}': {}", collectionName,
                    requestJson.length() > 500 ? requestJson.substring(0, 500) + "..." : requestJson);

            String response = qdrantWebClient.put()
                    .uri("/collections/{collection_name}/points", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully stored {} vector points to collection '{}'. Response: {}",
                    points.size(), collectionName, response);

        } catch (WebClientResponseException e) {
            log.error("Qdrant API error while storing vectors: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量存储失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to store vector points to collection '{}': {}",
                    collectionName, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量存储失败: " + e.getMessage());
        }
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "查询向量不能为空");
        }
        if (topK <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "topK 必须大于 0");
        }

        String collectionName = qdrantProperties.getCollectionName();
        log.info("Searching in collection '{}' with topK={}", collectionName, topK);

        try {
            // Build the search request body
            // POST /collections/{collection_name}/points/search
            // Body: {"vector": [...], "limit": 5, "with_payload": true}
            JSONObject requestBody = new JSONObject();
            requestBody.set("vector", queryVector);
            requestBody.set("limit", topK);
            requestBody.set("with_payload", true);

            String requestJson = requestBody.toString();
            log.debug("Search request to collection '{}': vector_dim={}, topK={}",
                    collectionName, queryVector.length, topK);

            String response = qdrantWebClient.post()
                    .uri("/collections/{collection_name}/points/search", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                log.warn("Empty response from Qdrant search");
                return Collections.emptyList();
            }

            // Parse the response
            JSONObject responseObj = JSONUtil.parseObj(response);
            JSONArray resultArray = responseObj.getJSONArray("result");

            if (resultArray == null || resultArray.isEmpty()) {
                log.info("No search results found in collection '{}'", collectionName);
                return Collections.emptyList();
            }

            List<SearchResult> results = new ArrayList<>();
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject resultObj = resultArray.getJSONObject(i);

                SearchResult searchResult = new SearchResult();
                // Qdrant returns id as either string or number
                Object idObj = resultObj.getObj("id");
                searchResult.setId(idObj != null ? idObj.toString() : null);
                searchResult.setScore(resultObj.getFloat("score"));

                // Parse payload
                JSONObject payloadObj = resultObj.getJSONObject("payload");
                if (payloadObj != null) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    for (String key : payloadObj.keySet()) {
                        payload.put(key, payloadObj.getObj(key));
                    }
                    searchResult.setPayload(payload);
                }

                results.add(searchResult);
            }

            log.info("Found {} search results in collection '{}'", results.size(), collectionName);
            return results;

        } catch (WebClientResponseException e) {
            log.error("Qdrant API error while searching: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量搜索失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to search in collection '{}': {}",
                    collectionName, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量搜索失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteVector(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "向量ID不能为空");
        }

        String collectionName = qdrantProperties.getCollectionName();
        log.info("Deleting vector point '{}' from collection '{}'", id, collectionName);

        try {
            // POST /collections/{collection_name}/points/delete
            // Body: {"points": ["uuid"]}
            JSONObject requestBody = new JSONObject();
            requestBody.set("points", new String[]{id});

            String requestJson = requestBody.toString();

            String response = qdrantWebClient.post()
                    .uri("/collections/{collection_name}/points/delete", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully deleted vector point '{}' from collection '{}'. Response: {}",
                    id, collectionName, response);

        } catch (WebClientResponseException e) {
            log.error("Qdrant API error while deleting vector: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量删除失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete vector point '{}' from collection '{}': {}",
                    id, collectionName, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "向量删除失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "文档ID不能为空");
        }

        String collectionName = qdrantProperties.getCollectionName();
        log.info("Deleting all vector points for document_id={} from collection '{}'",
                documentId, collectionName);

        try {
            // POST /collections/{collection_name}/points/delete
            // Body: {"filter": {"must": [{"key": "document_id", "match": {"value": 123}}]}}
            JSONObject requestBody = new JSONObject();
            JSONObject filter = new JSONObject();
            JSONArray must = new JSONArray();

            JSONObject condition = new JSONObject();
            condition.set("key", "document_id");
            JSONObject match = new JSONObject();
            match.set("value", documentId);
            condition.set("match", match);

            must.add(condition);
            filter.set("must", must);
            requestBody.set("filter", filter);

            String requestJson = requestBody.toString();
            log.debug("Delete by document_id request: {}", requestJson);

            String response = qdrantWebClient.post()
                    .uri("/collections/{collection_name}/points/delete", collectionName)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Successfully deleted vector points for document_id={} from collection '{}'. Response: {}",
                    documentId, collectionName, response);

        } catch (WebClientResponseException e) {
            log.error("Qdrant API error while deleting by document_id: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "按文档ID删除向量失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete vector points for document_id={} from collection '{}': {}",
                    documentId, collectionName, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "按文档ID删除向量失败: " + e.getMessage());
        }
    }
}
