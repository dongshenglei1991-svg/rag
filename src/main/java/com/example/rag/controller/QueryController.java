package com.example.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.entity.QueryHistory;
import com.example.rag.mapper.QueryHistoryMapper;
import com.example.rag.service.QueryService;
import com.example.rag.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询控制器
 * 提供查询问答和查询历史API接口
 *
 * 验证需求：7.4
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    private QueryService queryService;

    @Autowired
    private QueryHistoryMapper queryHistoryMapper;

    /**
     * 提交查询
     * POST /api/query
     *
     * @param request 查询请求（包含查询文本和可选的topK参数）
     * @return 查询响应（包含答案和引用的文档片段）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QueryResponseVO>> query(
            @RequestBody QueryRequest request) {
        log.info("接收查询请求，查询文本：{}", request.getQuery());

        // 参数验证：查询文本不能为空
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("查询文本为空");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "查询文本不能为空"));
        }

        try {
            QueryResponseVO responseVO = queryService.query(request.getQuery().trim(), request.getTopK());
            log.info("查询成功，响应时间：{}ms", responseVO.getResponseTimeMs());
            return ResponseEntity.ok(ApiResponse.success(responseVO));
        } catch (Exception e) {
            log.error("查询处理失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "查询处理失败，请稍后重试"));
        }
    }

    /**
     * 获取查询历史（分页）
     * GET /api/query/history?page=1&size=20
     *
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认20）
     * @return 查询历史列表
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResult<QueryHistoryVO>>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("接收查询历史请求，页码：{}，每页大小：{}", page, size);

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

        // 使用MyBatis-Plus分页查询，按查询时间倒序
        Page<QueryHistory> pageParam = new Page<>(page, size);
        QueryWrapper<QueryHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("query_time");

        Page<QueryHistory> resultPage = queryHistoryMapper.selectPage(pageParam, queryWrapper);

        // 转换为VO列表
        List<QueryHistoryVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<QueryHistoryVO> pageResult = new PageResult<>();
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setRecords(voList);

        log.info("查询历史获取成功，总记录数：{}", resultPage.getTotal());
        return ResponseEntity.ok(ApiResponse.success(pageResult));
    }

    /**
     * 将QueryHistory实体转换为QueryHistoryVO
     *
     * @param entity 查询历史实体
     * @return 查询历史VO
     */
    private QueryHistoryVO convertToVO(QueryHistory entity) {
        QueryHistoryVO vo = new QueryHistoryVO();
        vo.setId(entity.getId());
        vo.setQueryText(entity.getQueryText());
        vo.setAnswer(entity.getAnswer());
        vo.setQueryTime(entity.getQueryTime());
        vo.setResponseTimeMs(entity.getResponseTimeMs());
        return vo;
    }
}
