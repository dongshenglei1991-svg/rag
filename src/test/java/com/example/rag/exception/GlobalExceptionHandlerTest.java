package com.example.rag.exception;

import com.example.rag.vo.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.springframework.http.HttpHeaders;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    @Test
    void testHandleBusinessException() {
        BusinessException exception = new BusinessException(400, "业务错误", HttpStatus.BAD_REQUEST);
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
        assertEquals("业务错误", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        assertNotNull(response.getBody().getTimestamp());
    }
    
    @Test
    void testHandleBusinessExceptionWithDifferentHttpStatus() {
        BusinessException exception = new BusinessException(404, "资源不存在", HttpStatus.NOT_FOUND);
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getCode());
    }
    
    @Test
    void testHandleBusinessExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        BusinessException exception = new BusinessException(500, "服务异常", HttpStatus.INTERNAL_SERVER_ERROR, cause);
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("服务异常", response.getBody().getMessage());
    }
    
    @Test
    void testHandleValidationException() {
        // Create a real BindException instead of mocking
        BindException exception = new BindException(new Object(), "testObject");
        exception.addError(new FieldError("testObject", "testField", "字段验证失败"));
        
        // 使用 Spring 6.0 兼容的构造函数
        MethodArgumentNotValidException methodException = new MethodArgumentNotValidException(
                (org.springframework.core.MethodParameter) null, 
                exception.getBindingResult()
        );
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(methodException);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("参数验证失败"));
    }
    
    @Test
    void testHandleBindException() {
        BindException exception = new BindException(new Object(), "object");
        exception.addError(new FieldError("object", "field", "绑定失败"));
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBindException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("参数绑定失败"));
    }
    
    @Test
    void testHandleMaxUploadSizeExceededException() {
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(52428800);
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMaxUploadSizeExceededException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("文件大小超过限制"));
    }
    
    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("非法参数");
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleIllegalArgumentException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("参数错误"));
    }
    
    @Test
    void testHandleNullPointerException() {
        NullPointerException exception = new NullPointerException("空指针");
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleNullPointerException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("系统内部错误，请稍后重试", response.getBody().getMessage());
    }
    
    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("未知错误");
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("系统错误，请稍后重试", response.getBody().getMessage());
    }
    
    @Test
    void testHandleWebClientResponseException() {
        // Create a WebClientResponseException simulating a 503 from external API
        WebClientResponseException exception = WebClientResponseException.create(
                503,
                "Service Unavailable",
                null,
                "{\"error\":\"model overloaded\"}".getBytes(),
                null
        );
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleWebClientResponseException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getCode());
        assertEquals("外部服务暂时不可用，请稍后重试", response.getBody().getMessage());
        // Verify no technical details leak to user
        assertFalse(response.getBody().getMessage().contains("Service Unavailable"));
        assertFalse(response.getBody().getMessage().contains("model overloaded"));
    }
    
    @Test
    void testHandleWebClientResponseExceptionWith4xxStatus() {
        // Test with a 401 Unauthorized response
        WebClientResponseException exception = WebClientResponseException.create(
                401,
                "Unauthorized",
                null,
                "{\"error\":\"invalid api key\"}".getBytes(),
                null
        );
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleWebClientResponseException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getCode());
        assertEquals("外部服务暂时不可用，请稍后重试", response.getBody().getMessage());
        // Verify API key details are not exposed
        assertFalse(response.getBody().getMessage().contains("api key"));
        assertFalse(response.getBody().getMessage().contains("Unauthorized"));
    }
    
    @Test
    void testHandleWebClientRequestException() {
        // Create a WebClientRequestException simulating a connection failure
        WebClientRequestException exception = new WebClientRequestException(
                new java.net.ConnectException("Connection refused"),
                HttpMethod.POST,
                URI.create("https://openrouter.ai/api/v1/embeddings"),
                HttpHeaders.EMPTY
        );
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleWebClientRequestException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getCode());
        assertEquals("外部服务连接失败，请稍后重试", response.getBody().getMessage());
        // Verify no technical details leak to user
        assertFalse(response.getBody().getMessage().contains("Connection refused"));
        assertFalse(response.getBody().getMessage().contains("openrouter"));
    }
    
    @Test
    void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Unexpected runtime error");
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleRuntimeException(exception);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("系统处理异常，请稍后重试", response.getBody().getMessage());
        // Verify no technical details leak to user
        assertFalse(response.getBody().getMessage().contains("Unexpected runtime error"));
    }
    
    @Test
    void testErrorResponseDoesNotContainTechnicalDetails() {
        // 测试错误响应不包含技术细节（如堆栈跟踪）
        Exception exception = new Exception("Internal technical error with stack trace");
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(exception);
        
        // 验证返回的消息是用户友好的，不包含技术细节
        assertFalse(response.getBody().getMessage().contains("stack trace"));
        assertFalse(response.getBody().getMessage().contains("Internal technical"));
        assertEquals("系统错误，请稍后重试", response.getBody().getMessage());
    }
    
    @Test
    void testWebClientExceptionsDoNotLeakApiDetails() {
        // Verify WebClientResponseException doesn't leak API URL or response body
        WebClientResponseException responseException = WebClientResponseException.create(
                500,
                "Internal Server Error",
                null,
                "Detailed error from OpenRouter API with sensitive info".getBytes(),
                null
        );
        
        ResponseEntity<ApiResponse<Void>> response1 = exceptionHandler.handleWebClientResponseException(responseException);
        assertFalse(response1.getBody().getMessage().contains("OpenRouter"));
        assertFalse(response1.getBody().getMessage().contains("sensitive"));
        assertFalse(response1.getBody().getMessage().contains("Internal Server Error"));
        
        // Verify WebClientRequestException doesn't leak connection details
        WebClientRequestException requestException = new WebClientRequestException(
                new java.net.UnknownHostException("openrouter.ai"),
                HttpMethod.POST,
                URI.create("https://openrouter.ai/api/v1/chat/completions"),
                HttpHeaders.EMPTY
        );
        
        ResponseEntity<ApiResponse<Void>> response2 = exceptionHandler.handleWebClientRequestException(requestException);
        assertFalse(response2.getBody().getMessage().contains("openrouter.ai"));
        assertFalse(response2.getBody().getMessage().contains("UnknownHostException"));
    }
    
    @Test
    void testAllErrorResponsesHaveUniformFormat() {
        // 测试所有错误响应都有统一的格式
        BusinessException businessException = new BusinessException("业务错误");
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("参数错误");
        Exception genericException = new Exception("系统错误");
        RuntimeException runtimeException = new RuntimeException("运行时错误");
        
        ResponseEntity<ApiResponse<Void>> response1 = exceptionHandler.handleBusinessException(businessException);
        ResponseEntity<ApiResponse<Void>> response2 = exceptionHandler.handleIllegalArgumentException(illegalArgumentException);
        ResponseEntity<ApiResponse<Void>> response3 = exceptionHandler.handleException(genericException);
        ResponseEntity<ApiResponse<Void>> response4 = exceptionHandler.handleRuntimeException(runtimeException);
        
        // 验证所有响应都有必需的字段
        for (ResponseEntity<ApiResponse<Void>> response : new ResponseEntity[]{response1, response2, response3, response4}) {
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getCode());
            assertNotNull(response.getBody().getMessage());
            assertNotNull(response.getBody().getTimestamp());
            assertNull(response.getBody().getData());
        }
    }
    
    @Test
    void testErrorResponsesHaveCorrectHttpStatusCodes() {
        // 测试错误响应有正确的 HTTP 状态码
        BusinessException badRequestException = new BusinessException(400, "错误请求", HttpStatus.BAD_REQUEST);
        BusinessException notFoundException = new BusinessException(404, "未找到", HttpStatus.NOT_FOUND);
        BusinessException serverErrorException = new BusinessException(500, "服务器错误", HttpStatus.INTERNAL_SERVER_ERROR);
        
        ResponseEntity<ApiResponse<Void>> response1 = exceptionHandler.handleBusinessException(badRequestException);
        ResponseEntity<ApiResponse<Void>> response2 = exceptionHandler.handleBusinessException(notFoundException);
        ResponseEntity<ApiResponse<Void>> response3 = exceptionHandler.handleBusinessException(serverErrorException);
        
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, response2.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response3.getStatusCode());
    }
    
    @Test
    void testWebClientResponseExceptionReturns502() {
        WebClientResponseException exception = WebClientResponseException.create(
                429, "Too Many Requests", null, null, null);
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleWebClientResponseException(exception);
        
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getCode());
    }
    
    @Test
    void testWebClientRequestExceptionReturns502() {
        WebClientRequestException exception = new WebClientRequestException(
                new java.io.IOException("Connection timed out"),
                HttpMethod.GET,
                URI.create("https://example.com/api"),
                HttpHeaders.EMPTY
        );
        
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleWebClientRequestException(exception);
        
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().getCode());
    }
}
