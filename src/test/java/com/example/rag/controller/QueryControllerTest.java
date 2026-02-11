package com.example.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rag.entity.QueryHistory;
import com.example.rag.mapper.QueryHistoryMapper;
import com.example.rag.service.QueryService;
import com.example.rag.vo.ChunkReference;
import com.example.rag.vo.QueryResponseVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QueryController 单元测试
 * 测试查询问答和查询历史API接口
 *
 * 验证需求：7.4
 */
@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueryService queryService;

    @MockBean
    private QueryHistoryMapper queryHistoryMapper;

    private QueryResponseVO mockQueryResponse;
    private Page<QueryHistory> mockHistoryPage;

    @BeforeEach
    void setUp() {
        // 准备查询响应数据
        List<ChunkReference> references = new ArrayList<>();
        references.add(new ChunkReference(1L, "民法典.pdf",
                "第十三条 自然人从出生时起到死亡时止，具有民事权利能力。", 0.92f));

        mockQueryResponse = new QueryResponseVO();
        mockQueryResponse.setQuery("什么是民事权利能力？");
        mockQueryResponse.setAnswer("民事权利能力是指民事主体依法享有民事权利和承担民事义务的资格。");
        mockQueryResponse.setReferences(references);
        mockQueryResponse.setResponseTimeMs(1500L);

        // 准备查询历史分页数据
        List<QueryHistory> historyRecords = new ArrayList<>();
        QueryHistory history1 = new QueryHistory();
        history1.setId(1L);
        history1.setQueryText("什么是民事权利能力？");
        history1.setAnswer("民事权利能力是指...");
        history1.setQueryTime(LocalDateTime.of(2024, 1, 15, 10, 35, 0));
        history1.setResponseTimeMs(1500);
        historyRecords.add(history1);

        QueryHistory history2 = new QueryHistory();
        history2.setId(2L);
        history2.setQueryText("合同的成立条件是什么？");
        history2.setAnswer("合同的成立需要...");
        history2.setQueryTime(LocalDateTime.of(2024, 1, 15, 11, 0, 0));
        history2.setResponseTimeMs(2000);
        historyRecords.add(history2);

        mockHistoryPage = new Page<>(1, 20);
        mockHistoryPage.setRecords(historyRecords);
        mockHistoryPage.setTotal(2L);
    }

    /**
     * 测试提交查询 - 成功场景
     */
    @Test
    void testQuery_Success() throws Exception {
        when(queryService.query(eq("什么是民事权利能力？"), isNull()))
                .thenReturn(mockQueryResponse);

        Map<String, Object> request = new HashMap<>();
        request.put("query", "什么是民事权利能力？");

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.query").value("什么是民事权利能力？"))
                .andExpect(jsonPath("$.data.answer").value("民事权利能力是指民事主体依法享有民事权利和承担民事义务的资格。"))
                .andExpect(jsonPath("$.data.responseTimeMs").value(1500))
                .andExpect(jsonPath("$.data.references[0].documentId").value(1))
                .andExpect(jsonPath("$.data.references[0].documentName").value("民法典.pdf"))
                .andExpect(jsonPath("$.data.references[0].score").value(0.92));
    }

    /**
     * 测试提交查询 - 带topK参数
     */
    @Test
    void testQuery_WithTopK() throws Exception {
        when(queryService.query(eq("什么是民事权利能力？"), eq(3)))
                .thenReturn(mockQueryResponse);

        Map<String, Object> request = new HashMap<>();
        request.put("query", "什么是民事权利能力？");
        request.put("topK", 3);

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.query").value("什么是民事权利能力？"));
    }

    /**
     * 测试提交查询 - 查询文本为空
     */
    @Test
    void testQuery_EmptyQuery() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("query", "");

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("查询文本不能为空"));
    }

    /**
     * 测试提交查询 - 查询文本为null
     */
    @Test
    void testQuery_NullQuery() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("topK", 5);

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("查询文本不能为空"));
    }

    /**
     * 测试提交查询 - 仅空白字符
     */
    @Test
    void testQuery_WhitespaceOnly() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("query", "   ");

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("查询文本不能为空"));
    }

    /**
     * 测试提交查询 - 服务异常
     */
    @Test
    void testQuery_ServiceException() throws Exception {
        when(queryService.query(anyString(), any()))
                .thenThrow(new RuntimeException("向量化服务调用失败"));

        Map<String, Object> request = new HashMap<>();
        request.put("query", "测试查询");

        mockMvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("查询处理失败，请稍后重试"));
    }

    /**
     * 测试获取查询历史 - 成功场景
     */
    @Test
    void testGetHistory_Success() throws Exception {
        when(queryHistoryMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(mockHistoryPage);

        mockMvc.perform(get("/api/query/history")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].queryText").value("什么是民事权利能力？"))
                .andExpect(jsonPath("$.data.records[0].responseTimeMs").value(1500))
                .andExpect(jsonPath("$.data.records[1].id").value(2))
                .andExpect(jsonPath("$.data.records[1].queryText").value("合同的成立条件是什么？"));
    }

    /**
     * 测试获取查询历史 - 使用默认参数
     */
    @Test
    void testGetHistory_DefaultParams() throws Exception {
        when(queryHistoryMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(mockHistoryPage);

        mockMvc.perform(get("/api/query/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    /**
     * 测试获取查询历史 - 空结果
     */
    @Test
    void testGetHistory_EmptyResult() throws Exception {
        Page<QueryHistory> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(new ArrayList<>());
        emptyPage.setTotal(0L);

        when(queryHistoryMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/query/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    /**
     * 测试获取查询历史 - 无效的页码参数
     */
    @Test
    void testGetHistory_InvalidPage() throws Exception {
        mockMvc.perform(get("/api/query/history")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("页码必须大于0"));
    }

    /**
     * 测试获取查询历史 - 无效的每页大小参数（过大）
     */
    @Test
    void testGetHistory_InvalidSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/query/history")
                        .param("page", "1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("每页大小必须在1-100之间"));
    }

    /**
     * 测试获取查询历史 - 无效的每页大小参数（过小）
     */
    @Test
    void testGetHistory_InvalidSizeTooSmall() throws Exception {
        mockMvc.perform(get("/api/query/history")
                        .param("page", "1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("每页大小必须在1-100之间"));
    }
}
