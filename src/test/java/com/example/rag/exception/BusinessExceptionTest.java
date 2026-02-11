package com.example.rag.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BusinessException 单元测试
 */
class BusinessExceptionTest {
    
    @Test
    void testConstructorWithCodeMessageAndHttpStatus() {
        Integer code = 400;
        String message = "测试错误";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        
        BusinessException exception = new BusinessException(code, message, httpStatus);
        
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(httpStatus, exception.getHttpStatus());
    }
    
    @Test
    void testConstructorWithCodeAndMessage() {
        Integer code = 400;
        String message = "测试错误";
        
        BusinessException exception = new BusinessException(code, message);
        
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
    
    @Test
    void testConstructorWithMessageOnly() {
        String message = "测试错误";
        
        BusinessException exception = new BusinessException(message);
        
        assertEquals(400, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
    
    @Test
    void testConstructorWithCause() {
        Integer code = 500;
        String message = "测试错误";
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        Throwable cause = new RuntimeException("原因异常");
        
        BusinessException exception = new BusinessException(code, message, httpStatus, cause);
        
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(httpStatus, exception.getHttpStatus());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void testUnsupportedFileFormat() {
        String format = "exe";
        
        BusinessException exception = BusinessException.unsupportedFileFormat(format);
        
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains(format));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
    
    @Test
    void testFileTooLarge() {
        long maxSize = 52428800; // 50MB
        
        BusinessException exception = BusinessException.fileTooLarge(maxSize);
        
        assertEquals(413, exception.getCode());
        assertTrue(exception.getMessage().contains("50"));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getHttpStatus());
    }
    
    @Test
    void testResourceNotFound() {
        String resourceName = "Document";
        Long id = 123L;
        
        BusinessException exception = BusinessException.resourceNotFound(resourceName, id);
        
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains(resourceName));
        assertTrue(exception.getMessage().contains(id.toString()));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }
    
    @Test
    void testExternalServiceError() {
        String serviceName = "OpenRouter";
        String errorMessage = "API 调用超时";
        
        BusinessException exception = BusinessException.externalServiceError(serviceName, errorMessage);
        
        assertEquals(502, exception.getCode());
        assertTrue(exception.getMessage().contains(serviceName));
        assertTrue(exception.getMessage().contains(errorMessage));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }
    
    @Test
    void testServiceUnavailable() {
        String message = "服务暂时不可用";
        
        BusinessException exception = BusinessException.serviceUnavailable(message);
        
        assertEquals(503, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }
}
