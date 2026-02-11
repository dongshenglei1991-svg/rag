package com.example.rag.service.impl;

import com.example.rag.config.RagDocumentProperties;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.mapper.DocumentChunkMapper;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.DocumentProcessingService;
import com.example.rag.service.DocumentProcessor;
import com.example.rag.service.EmbeddingService;
import com.example.rag.service.VectorStoreService;
import com.example.rag.vo.VectorPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档处理服务实现类
 * 使用 @Async 实现异步文档处理
 * 流程：提取文本 → 分割片段 → 向量化 → 存储向量 → 更新文档状态
 *
 * 验证需求：2.1, 2.2, 2.3, 2.4, 3.1, 3.4, 10.2
 */
@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingServiceImpl.class);

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private DocumentProcessor documentProcessor;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private RagDocumentProperties ragDocumentProperties;

    /**
     * 异步处理文档
     * 使用 @Async 注解确保在独立线程中执行，不阻塞上传请求
     *
     * @param documentId 文档ID
     */
    @Override
    @Async
    public void processDocument(Long documentId) {
        log.info("开始异步处理文档，文档ID：{}", documentId);

        try {
            // 1. 从数据库加载文档信息
            Document document = documentMapper.selectById(documentId);
            if (document == null) {
                log.error("文档不存在，文档ID：{}", documentId);
                return;
            }

            log.info("加载文档成功：{}，文件路径：{}", document.getFileName(), document.getFilePath());

            // 2. 提取文档文本内容
            String text = documentProcessor.extractText(new File(document.getFilePath()));
            log.info("文本提取完成，文档ID：{}，文本长度：{} 字符", documentId, text.length());

            // 3. 将文本分割为片段
            int chunkSize = ragDocumentProperties.getChunkSize();
            int chunkOverlap = ragDocumentProperties.getChunkOverlap();
            List<String> chunks = documentProcessor.splitDocument(text, chunkSize, chunkOverlap);
            log.info("文档分割完成，文档ID：{}，片段数量：{}", documentId, chunks.size());

            // 4. 为每个片段生成向量并准备存储数据
            List<VectorPoint> vectorPoints = new ArrayList<>();
            List<DocumentChunk> documentChunks = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                String vectorId = UUID.randomUUID().toString();

                // 向量化片段
                float[] vector = embeddingService.embed(chunkContent);

                // 构建向量点的元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("document_id", documentId);
                metadata.put("chunk_index", i);
                metadata.put("content", chunkContent);
                metadata.put("document_name", document.getFileName());

                // 创建 VectorPoint
                VectorPoint vectorPoint = new VectorPoint(vectorId, vector, metadata);
                vectorPoints.add(vectorPoint);

                // 创建 DocumentChunk 记录
                DocumentChunk documentChunk = new DocumentChunk();
                documentChunk.setDocumentId(documentId);
                documentChunk.setChunkIndex(i);
                documentChunk.setContent(chunkContent);
                documentChunk.setVectorId(vectorId);
                documentChunk.setCharCount(chunkContent.length());
                documentChunks.add(documentChunk);

                log.debug("片段 {} 向量化完成，vectorId：{}", i, vectorId);
            }

            // 5. 批量存储向量到 Qdrant
            if (!vectorPoints.isEmpty()) {
                vectorStoreService.storeVectorBatch(vectorPoints);
                log.info("向量批量存储完成，文档ID：{}，向量数量：{}", documentId, vectorPoints.size());
            }

            // 6. 保存 DocumentChunk 记录到数据库
            for (DocumentChunk chunk : documentChunks) {
                documentChunkMapper.insert(chunk);
            }
            log.info("片段记录保存完成，文档ID：{}，片段数量：{}", documentId, documentChunks.size());

            // 7. 更新文档状态为 COMPLETED
            document.setStatus("COMPLETED");
            document.setChunkCount(chunks.size());
            document.setProcessTime(LocalDateTime.now());
            documentMapper.updateById(document);

            log.info("文档处理完成，文档ID：{}，文件名：{}，片段数量：{}",
                    documentId, document.getFileName(), chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败，文档ID：{}", documentId, e);

            // 更新文档状态为 FAILED，记录错误信息
            try {
                Document document = documentMapper.selectById(documentId);
                if (document != null) {
                    document.setStatus("FAILED");
                    document.setErrorMessage(e.getMessage());
                    document.setProcessTime(LocalDateTime.now());
                    documentMapper.updateById(document);
                    log.info("文档状态已更新为 FAILED，文档ID：{}", documentId);
                }
            } catch (Exception updateException) {
                log.error("更新文档失败状态时发生异常，文档ID：{}", documentId, updateException);
            }
        }
    }
}
