package com.example.rag.service.impl;

import com.example.rag.config.OpenRouterProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.LLMService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMService 单元测试
 * 使用 MockWebServer 模拟 OpenRouter Chat Completions API
 */
class LLMServiceImplTest {

    private MockWebServer mockWebServer;
    private LLMService llmService;
    private OpenRouterProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        // 启动 Mock 服务器
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 配置属性
        properties = new OpenRouterProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl(mockWebServer.url("/").toString());
        properties.setChatModel("openai/gpt-4");
        properties.setTimeout(5000);
        properties.setMaxRetries(3);

        // 创建 WebClient
        WebClient webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        // 创建服务实例
        llmService = new LLMServiceImpl(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ==================== buildPrompt 测试 ====================

    /**
     * 测试构建提示词 - 包含上下文
     */
    @Test
    void testBuildPrompt_WithContext() {
        List<DocumentChunk> context = createMockChunks(
                "第一条 民法调整平等主体的自然人、法人和非法人组织之间的人身关系和财产关系。",
                "第二条 民法所称的民事主体，是指自然人、法人和非法人组织。"
        );

        String prompt = llmService.buildPrompt("什么是民事主体？", context);

        // 验证提示词包含参考资料
        assertTrue(prompt.contains("参考资料："));
        assertTrue(prompt.contains("[1]"));
        assertTrue(prompt.contains("[2]"));
        assertTrue(prompt.contains("第一条"));
        assertTrue(prompt.contains("第二条"));
        // 验证包含用户问题
        assertTrue(prompt.contains("用户问题：什么是民事主体？"));
    }

    /**
     * 测试构建提示词 - 空上下文
     */
    @Test
    void testBuildPrompt_EmptyContext() {
        String prompt = llmService.buildPrompt("什么是民事主体？", Collections.emptyList());

        assertTrue(prompt.contains("参考资料：无"));
        assertTrue(prompt.contains("用户问题：什么是民事主体？"));
    }

    /**
     * 测试构建提示词 - null 上下文
     */
    @Test
    void testBuildPrompt_NullContext() {
        String prompt = llmService.buildPrompt("什么是民事主体？", null);

        assertTrue(prompt.contains("参考资料：无"));
        assertTrue(prompt.contains("用户问题：什么是民事主体？"));
    }

    /**
     * 测试构建提示词 - 单个上下文
     */
    @Test
    void testBuildPrompt_SingleContext() {
        List<DocumentChunk> context = createMockChunks("这是唯一的参考内容。");

        String prompt = llmService.buildPrompt("问题", context);

        assertTrue(prompt.contains("[1] 这是唯一的参考内容。"));
        assertFalse(prompt.contains("[2]"));
    }

    // ==================== generateAnswer 测试 ====================

    /**
     * 测试成功生成答案
     */
    @Test
    void testGenerateAnswer_Success() throws Exception {
        String responseBody = buildMockChatResponse("民事主体是指自然人、法人和非法人组织。");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        List<DocumentChunk> context = createMockChunks("第二条 民法所称的民事主体，是指自然人、法人和非法人组织。");

        String answer = llmService.generateAnswer("什么是民事主体？", context);

        assertNotNull(answer);
        assertEquals("民事主体是指自然人、法人和非法人组织。", answer);

        // 验证请求
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/chat/completions", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("openai/gpt-4"));
        assertTrue(body.contains("system"));
        assertTrue(body.contains("user"));
    }

    /**
     * 测试使用指定模型生成答案
     */
    @Test
    void testGenerateAnswer_WithSpecificModel() throws Exception {
        String responseBody = buildMockChatResponse("答案内容");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        List<DocumentChunk> context = createMockChunks("参考内容");

        String answer = llmService.generateAnswer("问题", context, "anthropic/claude-3-sonnet");

        assertNotNull(answer);
        assertEquals("答案内容", answer);

        // 验证请求中使用了指定的模型
        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("anthropic/claude-3-sonnet"));
    }

    /**
     * 测试空上下文生成答案
     */
    @Test
    void testGenerateAnswer_EmptyContext() throws Exception {
        String responseBody = buildMockChatResponse("参考资料中没有相关信息。");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        String answer = llmService.generateAnswer("什么是民事主体？", Collections.emptyList());

        assertNotNull(answer);
        assertEquals("参考资料中没有相关信息。", answer);
    }

    /**
     * 测试空查询抛出异常
     */
    @Test
    void testGenerateAnswer_EmptyQuery() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            llmService.generateAnswer("", Collections.emptyList());
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("查询内容不能为空"));
    }

    /**
     * 测试 null 查询抛出异常
     */
    @Test
    void testGenerateAnswer_NullQuery() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            llmService.generateAnswer(null, Collections.emptyList());
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("查询内容不能为空"));
    }

    /**
     * 测试 API 返回错误状态码（所有重试都失败）
     */
    @Test
    void testGenerateAnswer_ApiError() {
        // 需要为所有重试（初始 + maxRetries 次）都准备错误响应
        for (int i = 0; i <= properties.getMaxRetries(); i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"));
        }

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            llmService.generateAnswer("问题", Collections.emptyList());
        });

        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("大语言模型服务调用失败"));
    }

    /**
     * 测试 API 返回空 choices
     */
    @Test
    void testGenerateAnswer_EmptyChoices() {
        String responseBody = """
                {
                    "id": "chatcmpl-xxx",
                    "object": "chat.completion",
                    "choices": [],
                    "usage": {
                        "prompt_tokens": 100,
                        "completion_tokens": 0,
                        "total_tokens": 100
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            llmService.generateAnswer("问题", Collections.emptyList());
        });

        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("大语言模型服务返回空结果"));
    }

    /**
     * 测试 API 返回 null message content
     */
    @Test
    void testGenerateAnswer_NullMessageContent() {
        String responseBody = """
                {
                    "id": "chatcmpl-xxx",
                    "object": "chat.completion",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": null
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 100,
                        "completion_tokens": 0,
                        "total_tokens": 100
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            llmService.generateAnswer("问题", Collections.emptyList());
        });

        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains("大语言模型服务返回的消息内容为空"));
    }

    /**
     * 测试重试机制 - 重试后成功
     */
    @Test
    void testGenerateAnswer_RetrySuccess() throws Exception {
        // 第一次请求失败
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // 第二次请求失败
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // 第三次请求成功
        String responseBody = buildMockChatResponse("重试后的答案");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        String answer = llmService.generateAnswer("问题", Collections.emptyList());

        assertNotNull(answer);
        assertEquals("重试后的答案", answer);

        // 验证重试了 3 次
        assertEquals(3, mockWebServer.getRequestCount());
    }

    /**
     * 测试默认模型使用
     */
    @Test
    void testGenerateAnswer_UsesDefaultModel() throws Exception {
        String responseBody = buildMockChatResponse("答案");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        llmService.generateAnswer("问题", Collections.emptyList());

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("openai/gpt-4"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 Mock DocumentChunk 列表
     */
    private List<DocumentChunk> createMockChunks(String... contents) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setId((long) (i + 1));
            chunk.setDocumentId(1L);
            chunk.setChunkIndex(i);
            chunk.setContent(contents[i]);
            chunk.setVectorId("vec-" + (i + 1));
            chunk.setCharCount(contents[i].length());
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * 构建 Mock Chat Completions 响应
     */
    private String buildMockChatResponse(String answerContent) {
        // Escape special characters for JSON
        String escapedContent = answerContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        return """
                {
                    "id": "chatcmpl-test123",
                    "object": "chat.completion",
                    "choices": [
                        {
                            "index": 0,
                            "message": {
                                "role": "assistant",
                                "content": "%s"
                            },
                            "finish_reason": "stop"
                        }
                    ],
                    "usage": {
                        "prompt_tokens": 100,
                        "completion_tokens": 50,
                        "total_tokens": 150
                    }
                }
                """.formatted(escapedContent);
    }
}
