package com.example.rag.service.impl;

import com.example.rag.config.OpenRouterProperties;
import com.example.rag.entity.DocumentChunk;
import com.example.rag.exception.BusinessException;
import com.example.rag.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大语言模型服务实现类
 * 使用 OpenRouter API 调用 Chat Completions 模型生成答案
 */
@Service
public class LLMServiceImpl implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMServiceImpl.class);

    /**
     * 系统提示词
     */
    private static final String SYSTEM_PROMPT = "你是一个专业的法律知识助手。请根据以下参考资料回答用户的问题。如果参考资料中没有相关信息，请如实告知。";

    private final WebClient webClient;
    private final OpenRouterProperties properties;
    private final Retry retrySpec;

    public LLMServiceImpl(WebClient openRouterWebClient,
                          OpenRouterProperties properties) {
        this.webClient = openRouterWebClient;
        this.properties = properties;

        // 配置重试策略：最多 maxRetries 次，指数退避
        this.retrySpec = Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .doBeforeRetry(retrySignal -> {
                    log.warn("Retrying LLM API call, attempt: {}, error: {}",
                            retrySignal.totalRetries() + 1,
                            retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("LLM API retry exhausted after {} attempts",
                            retrySignal.totalRetries());
                    return new BusinessException(
                            HttpStatus.BAD_GATEWAY.value(),
                            "大语言模型服务调用失败，已重试 " + retrySignal.totalRetries() + " 次",
                            HttpStatus.BAD_GATEWAY,
                            retrySignal.failure()
                    );
                });
    }

    /**
     * 构建提示词
     * 格式：参考资料（带编号的检索内容）+ 用户查询
     *
     * @param query   用户查询
     * @param context 检索到的文档片段列表
     * @return 构建好的用户消息提示词
     */
    @Override
    public String buildPrompt(String query, List<DocumentChunk> context) {
        StringBuilder prompt = new StringBuilder();

        // 添加参考资料
        if (context != null && !context.isEmpty()) {
            prompt.append("参考资料：\n");
            for (int i = 0; i < context.size(); i++) {
                DocumentChunk chunk = context.get(i);
                prompt.append("[").append(i + 1).append("] ").append(chunk.getContent()).append("\n\n");
            }
        } else {
            prompt.append("参考资料：无\n\n");
        }

        // 添加用户查询
        prompt.append("用户问题：").append(query);

        return prompt.toString();
    }

    /**
     * 生成答案（使用默认模型）
     *
     * @param query   用户查询
     * @param context 检索到的文档片段列表
     * @return 生成的答案
     * @throws Exception 生成失败时抛出异常
     */
    @Override
    public String generateAnswer(String query, List<DocumentChunk> context) throws Exception {
        return generateAnswer(query, context, properties.getChatModel());
    }

    /**
     * 生成答案（指定模型）
     *
     * @param query   用户查询
     * @param context 检索到的文档片段列表
     * @param model   模型名称
     * @return 生成的答案
     * @throws Exception 生成失败时抛出异常
     */
    @Override
    public String generateAnswer(String query, List<DocumentChunk> context, String model) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "查询内容不能为空");
        }

        log.info("Generating answer for query: {}, model: {}, context chunks: {}",
                query, model, context != null ? context.size() : 0);

        try {
            // 构建用户消息提示词
            String userPrompt = buildPrompt(query, context);

            // 构建 OpenAI 兼容的 Chat Completions 请求体
            Map<String, Object> requestBody = buildRequestBody(model, userPrompt);

            // 调用 OpenRouter Chat Completions API
            ChatCompletionResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .retryWhen(retrySpec)
                    .block();

            // 解析响应
            String answer = extractAnswer(response);

            log.info("Answer generated successfully, length: {}", answer.length());
            return answer;

        } catch (WebClientResponseException e) {
            log.error("LLM API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "大语言模型服务调用失败: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY,
                    e
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during answer generation", e);
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "答案生成过程发生错误: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    /**
     * 构建 Chat Completions 请求体
     */
    private Map<String, Object> buildRequestBody(String model, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();

        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);
        messages.add(systemMessage);

        // 用户消息（包含参考资料和查询）
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * 从响应中提取答案内容
     */
    private String extractAnswer(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "大语言模型服务返回空结果",
                    HttpStatus.BAD_GATEWAY
            );
        }

        ChatChoice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "大语言模型服务返回的消息内容为空",
                    HttpStatus.BAD_GATEWAY
            );
        }

        return choice.getMessage().getContent();
    }

    // ==================== 内部响应类 ====================

    /**
     * OpenRouter Chat Completions API 响应结构
     */
    static class ChatCompletionResponse {
        private String id;
        private String object;
        private List<ChatChoice> choices;
        private ChatUsage usage;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public List<ChatChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<ChatChoice> choices) {
            this.choices = choices;
        }

        public ChatUsage getUsage() {
            return usage;
        }

        public void setUsage(ChatUsage usage) {
            this.usage = usage;
        }
    }

    /**
     * Chat 选择项
     */
    static class ChatChoice {
        private int index;
        private ChatMessage message;
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    /**
     * Chat 消息
     */
    static class ChatMessage {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * API 使用统计
     */
    static class ChatUsage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
