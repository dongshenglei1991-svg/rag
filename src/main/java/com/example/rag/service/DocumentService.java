package com.example.rag.service;

import com.example.rag.vo.DocumentDetailVO;
import com.example.rag.vo.DocumentVO;
import com.example.rag.vo.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档服务接口
 * 负责文档管理的业务逻辑
 */
public interface DocumentService {
    
    /**
     * 处理文档上传
     * 
     * @param file 上传的文件
     * @return 文档视图对象
     * @throws IOException 文件操作异常
     */
    DocumentVO uploadDocument(MultipartFile file) throws IOException;
    
    /**
     * 获取文档列表（分页）
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<DocumentVO> listDocuments(int page, int size);
    
    /**
     * 删除文档
     * 删除文件系统中的文件、Qdrant中的向量、数据库中的记录
     * 
     * @param id 文档ID
     */
    void deleteDocument(Long id);
    
    /**
     * 获取文档详情
     * 包含文档基本信息和所有片段列表
     * 
     * @param id 文档ID
     * @return 文档详情视图对象
     */
    DocumentDetailVO getDocument(Long id);
}
