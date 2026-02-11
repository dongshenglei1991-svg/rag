package com.example.rag.controller;

import com.example.rag.service.DocumentService;
import com.example.rag.vo.ApiResponse;
import com.example.rag.vo.DocumentDetailVO;
import com.example.rag.vo.DocumentVO;
import com.example.rag.vo.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档管理控制器
 * 提供文档上传、查询、删除等API接口
 * 
 * 验证需求：7.1, 7.2, 7.3
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * 上传文档
     * POST /api/documents
     * 
     * @param file 上传的文件
     * @return 文档信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentVO>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        log.info("接收文档上传请求，文件名：{}", file.getOriginalFilename());
        
        try {
            DocumentVO documentVO = documentService.uploadDocument(file);
            log.info("文档上传成功，文档ID：{}", documentVO.getId());
            return ResponseEntity.ok(ApiResponse.success(documentVO));
        } catch (IOException e) {
            log.error("文档上传失败，IO异常", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "文件保存失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取文档列表（分页）
     * GET /api/documents?page=1&size=10
     * 
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认10）
     * @return 文档列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<DocumentVO>>> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("接收文档列表查询请求，页码：{}，每页大小：{}", page, size);
        
        // 参数验证
        if (page < 1) {
            log.warn("页码参数无效：{}", page);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "页码必须大于0"));
        }
        if (size < 1 || size > 100) {
            log.warn("每页大小参数无效：{}", size);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "每页大小必须在1-100之间"));
        }
        
        PageResult<DocumentVO> result = documentService.listDocuments(page, size);
        log.info("文档列表查询成功，总记录数：{}", result.getTotal());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 删除文档
     * DELETE /api/documents/{id}
     * 
     * @param id 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        log.info("接收文档删除请求，文档ID：{}", id);
        
        // 参数验证
        if (id == null || id <= 0) {
            log.warn("文档ID参数无效：{}", id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文档ID无效"));
        }
        
        documentService.deleteDocument(id);
        log.info("文档删除成功，文档ID：{}", id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 获取文档详情
     * GET /api/documents/{id}
     * 
     * @param id 文档ID
     * @return 文档详情（包含片段列表）
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDetailVO>> getDocument(@PathVariable Long id) {
        log.info("接收文档详情查询请求，文档ID：{}", id);
        
        // 参数验证
        if (id == null || id <= 0) {
            log.warn("文档ID参数无效：{}", id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文档ID无效"));
        }
        
        DocumentDetailVO detailVO = documentService.getDocument(id);
        log.info("文档详情查询成功，文档ID：{}，片段数量：{}", id, detailVO.getChunkCount());
        return ResponseEntity.ok(ApiResponse.success(detailVO));
    }
}
