package com.example.rag.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 单元测试
 */
class ApiResponseTest {
    
    @Test
    void testSuccessWithData() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);
        
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp() > 0);
    }
    
    @Test
    void testSuccessWithoutData() {
        ApiResponse<Void> response = ApiResponse.success();
        
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithCustomMessage() {
        String customMessage = "操作成功";
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(customMessage, data);
        
        assertEquals(200, response.getCode());
        assertEquals(customMessage, response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithCodeAndMessage() {
        Integer errorCode = 400;
        String errorMessage = "参数错误";
        ApiResponse<Void> response = ApiResponse.error(errorCode, errorMessage);
        
        assertEquals(errorCode, response.getCode());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithMessageOnly() {
        String errorMessage = "系统错误";
        ApiResponse<Void> response = ApiResponse.error(errorMessage);
        
        assertEquals(500, response.getCode());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testTimestampIsCurrentTime() {
        long beforeTimestamp = System.currentTimeMillis();
        ApiResponse<String> response = ApiResponse.success("test");
        long afterTimestamp = System.currentTimeMillis();
        
        assertTrue(response.getTimestamp() >= beforeTimestamp);
        assertTrue(response.getTimestamp() <= afterTimestamp);
    }
    
    @Test
    void testResponseFormatUniformity() {
        // 测试成功响应格式
        ApiResponse<String> successResponse = ApiResponse.success("data");
        assertNotNull(successResponse.getCode());
        assertNotNull(successResponse.getMessage());
        assertNotNull(successResponse.getTimestamp());
        
        // 测试错误响应格式
        ApiResponse<Void> errorResponse = ApiResponse.error(400, "error");
        assertNotNull(errorResponse.getCode());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }
}
