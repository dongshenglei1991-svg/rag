package com.example.rag.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.config.QdrantProperties;
import com.example.rag.config.RagDocumentProperties;
import com.example.rag.entity.Document;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.exception.BusinessException;
import com.example.rag.mapper.DocumentChunkMapper;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.DocumentProcessingService;
import com.example.rag.service.DocumentService;
import com.example.rag.service.QdrantClient;
import com.example.rag.vo.ChunkVO;
import com.example.rag.vo.DocumentDetailVO;
import com.example.rag.vo.DocumentVO;
import com.example.rag.vo.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 * 实现文档上传、验证、存储等功能
 */
@Service
public class DocumentServiceImpl implements DocumentService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    
    @Autowired
    private DocumentMapper documentMapper;
    
    @Autowired
    private DocumentChunkMapper documentChunkMapper;
    
    @Autowired
    private RagDocumentProperties ragDocumentProperties;
    
    @Autowired
    private QdrantClient qdrantClient;
    
    @Autowired
    private QdrantProperties qdrantProperties;
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    /**
     * 处理文档上传
     * 
     * @param file 上传的文件
     * @return 文档视图对象
     * @throws IOException 文件操作异常
     */
    @Override
    public DocumentVO uploadDocument(MultipartFile file) throws IOException {
        log.info("开始处理文档上传，文件名：{}", file.getOriginalFilename());
        
        // 1. 验证文件是否为空
        if (file.isEmpty()) {
            log.warn("上传文件为空");
            throw new BusinessException(400, "上传文件不能为空");
        }
        
        // 2. 验证文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            log.warn("文件名为空");
            throw new BusinessException(400, "文件名不能为空");
        }
        
        String fileExtension = FileNameUtil.extName(originalFilename);
        if (!ragDocumentProperties.isSupportedFormat(fileExtension)) {
            log.warn("不支持的文件格式：{}", fileExtension);
            throw new BusinessException(400, 
                String.format("不支持的文件格式：%s，支持的格式：%s", 
                    fileExtension, 
                    ragDocumentProperties.getSupportedFormats()));
        }
        
        // 3. 验证文件大小
        long fileSize = file.getSize();
        if (fileSize > ragDocumentProperties.getMaxFileSize()) {
            log.warn("文件过大：{} 字节，最大允许：{} 字节", 
                fileSize, ragDocumentProperties.getMaxFileSize());
            throw new BusinessException(413, 
                String.format("文件过大，最大允许 %d MB", 
                    ragDocumentProperties.getMaxFileSize() / 1024 / 1024));
        }
        
        // 4. 使用 Hutool FileUtil 保存文件到本地
        // 将相对路径转为绝对路径，避免 Tomcat 临时目录问题
        File uploadDirFile = new File(ragDocumentProperties.getUploadDir()).getAbsoluteFile();
        // 确保上传目录存在
        FileUtil.mkdir(uploadDirFile);
        
        // 生成唯一文件名：UUID_原始文件名
        String uniqueFileName = UUID.randomUUID().toString(true) + "_" + originalFilename;
        String filePath = FileUtil.normalize(uploadDirFile.getPath() + File.separator + uniqueFileName);
        
        // 保存文件
        File destFile = new File(filePath);
        file.transferTo(destFile.getAbsoluteFile());
        log.info("文件保存成功：{}", filePath);
        
        // 5. 创建 Document 记录，状态设为 PROCESSING
        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFileSize(fileSize);
        document.setFileType(file.getContentType());
        document.setFilePath(filePath);
        document.setStatus("PROCESSING");
        document.setUploadTime(LocalDateTime.now());
        document.setChunkCount(0);
        
        // 保存到数据库
        int result = documentMapper.insert(document);
        if (result <= 0) {
            log.error("文档记录保存失败");
            // 删除已保存的文件
            FileUtil.del(destFile);
            throw new BusinessException(500, "文档记录保存失败");
        }
        
        log.info("文档上传成功，文档ID：{}，文件名：{}", document.getId(), originalFilename);
        
        // 6. 触发异步文档处理
        documentProcessingService.processDocument(document.getId());
        log.info("已触发异步文档处理，文档ID：{}", document.getId());
        
        // 7. 返回 DocumentVO
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setFileName(document.getFileName());
        vo.setFileSize(document.getFileSize());
        vo.setFileType(document.getFileType());
        vo.setStatus(document.getStatus());
        vo.setUploadTime(document.getUploadTime());
        vo.setChunkCount(document.getChunkCount());
        
        return vo;
    }
    
    /**
     * 获取文档列表（分页）
     * 使用 MyBatis-Plus 分页查询，按上传时间倒序排列
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @Override
    public PageResult<DocumentVO> listDocuments(int page, int size) {
        log.info("查询文档列表，页码：{}，每页大小：{}", page, size);
        
        // 1. 创建 MyBatis-Plus 分页对象
        Page<Document> pageParam = new Page<>(page, size);
        
        // 2. 创建查询条件，按上传时间倒序排列
        QueryWrapper<Document> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("upload_time");
        
        // 3. 执行分页查询
        Page<Document> documentPage = documentMapper.selectPage(pageParam, queryWrapper);
        
        // 4. 转换为 DocumentVO 列表
        List<DocumentVO> voList = documentPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        // 5. 构建分页结果
        PageResult<DocumentVO> result = new PageResult<>();
        result.setTotal(documentPage.getTotal());
        result.setPage((int) documentPage.getCurrent());
        result.setSize((int) documentPage.getSize());
        result.setRecords(voList);
        
        log.info("查询文档列表成功，总记录数：{}，当前页记录数：{}", 
            result.getTotal(), voList.size());
        
        return result;
    }
    
    /**
     * 将 Document 实体转换为 DocumentVO
     * 
     * @param document 文档实体
     * @return 文档视图对象
     */
    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setFileName(document.getFileName());
        vo.setFileSize(document.getFileSize());
        vo.setFileType(document.getFileType());
        vo.setStatus(document.getStatus());
        vo.setUploadTime(document.getUploadTime());
        vo.setChunkCount(document.getChunkCount());
        return vo;
    }
    
    /**
     * 删除文档
     * 删除文件系统中的文件、Qdrant中的向量、数据库中的记录
     * 
     * @param id 文档ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        log.info("开始删除文档，文档ID：{}", id);
        
        // 1. 查询文档是否存在
        Document document = documentMapper.selectById(id);
        if (document == null) {
            log.warn("文档不存在，文档ID：{}", id);
            throw new BusinessException(404, "文档不存在");
        }
        
        log.info("找到文档：{}，文件路径：{}", document.getFileName(), document.getFilePath());
        
        // 2. 删除文件系统中的文件（使用 Hutool FileUtil）
        String filePath = document.getFilePath();
        if (filePath != null && !filePath.isEmpty()) {
            boolean fileDeleted = FileUtil.del(filePath);
            if (fileDeleted) {
                log.info("文件删除成功：{}", filePath);
            } else {
                log.warn("文件删除失败或文件不存在：{}", filePath);
            }
        }
        
        // 3. 删除 Qdrant 中的向量（通过 document_id 过滤）
        try {
            String collectionName = qdrantProperties.getCollectionName();
            boolean vectorsDeleted = qdrantClient.deletePointsByDocumentId(collectionName, id);
            if (vectorsDeleted) {
                log.info("Qdrant 向量删除成功，文档ID：{}", id);
            } else {
                log.warn("Qdrant 向量删除失败，文档ID：{}", id);
            }
        } catch (Exception e) {
            log.error("删除 Qdrant 向量时发生异常，文档ID：{}", id, e);
            // 继续执行，不中断删除流程
        }
        
        // 4. 删除数据库中的 Document 记录（级联删除 DocumentChunk）
        // 由于数据库设置了 ON DELETE CASCADE，删除 Document 会自动删除关联的 DocumentChunk
        int result = documentMapper.deleteById(id);
        if (result > 0) {
            log.info("文档记录删除成功，文档ID：{}，文件名：{}", id, document.getFileName());
        } else {
            log.error("文档记录删除失败，文档ID：{}", id);
            throw new BusinessException(500, "文档记录删除失败");
        }
        
        log.info("文档删除完成，文档ID：{}", id);
    }
    
    /**
     * 获取文档详情
     * 包含文档基本信息和所有片段列表
     * 
     * @param id 文档ID
     * @return 文档详情视图对象
     */
    @Override
    public DocumentDetailVO getDocument(Long id) {
        log.info("查询文档详情，文档ID：{}", id);
        
        // 1. 查询文档是否存在
        Document document = documentMapper.selectById(id);
        if (document == null) {
            log.warn("文档不存在，文档ID：{}", id);
            throw new BusinessException(404, "文档不存在");
        }
        
        // 2. 查询文档的所有片段
        QueryWrapper<DocumentChunk> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("document_id", id);
        queryWrapper.orderByAsc("chunk_index");
        List<DocumentChunk> chunks = documentChunkMapper.selectList(queryWrapper);
        
        // 3. 转换为 ChunkVO 列表
        List<ChunkVO> chunkVOList = chunks.stream()
            .map(this::convertToChunkVO)
            .collect(Collectors.toList());
        
        // 4. 构建 DocumentDetailVO
        DocumentDetailVO detailVO = new DocumentDetailVO();
        detailVO.setId(document.getId());
        detailVO.setFileName(document.getFileName());
        detailVO.setFileSize(document.getFileSize());
        detailVO.setFileType(document.getFileType());
        detailVO.setStatus(document.getStatus());
        detailVO.setUploadTime(document.getUploadTime());
        detailVO.setProcessTime(document.getProcessTime());
        detailVO.setChunkCount(document.getChunkCount());
        detailVO.setChunks(chunkVOList);
        
        log.info("查询文档详情成功，文档ID：{}，片段数量：{}", id, chunkVOList.size());
        
        return detailVO;
    }
    
    /**
     * 将 DocumentChunk 实体转换为 ChunkVO
     * 
     * @param chunk 文档片段实体
     * @return 片段视图对象
     */
    private ChunkVO convertToChunkVO(DocumentChunk chunk) {
        ChunkVO vo = new ChunkVO();
        vo.setId(chunk.getId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setCharCount(chunk.getCharCount());
        return vo;
    }
}
