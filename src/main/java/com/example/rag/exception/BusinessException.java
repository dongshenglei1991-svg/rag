package com.example.rag.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于封装业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final Integer code;
    
    /**
     * HTTP 状态码
     */
    private final HttpStatus httpStatus;
    
    /**
     * 构造函数
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param httpStatus HTTP 状态码
     */
    public BusinessException(Integer code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    /**
     * 构造函数（默认 HTTP 状态码为 400）
     * 
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 构造函数（默认错误码为 400，HTTP 状态码为 400）
     * 
     * @param message 错误消息
     */
    public BusinessException(String message) {
        this(400, message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 构造函数（带原因异常）
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param httpStatus HTTP 状态码
     * @param cause 原因异常
     */
    public BusinessException(Integer code, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * 创建文件格式不支持异常
     * 
     * @param format 文件格式
     * @return BusinessException
     */
    public static BusinessException unsupportedFileFormat(String format) {
        return new BusinessException(400, "不支持的文件格式: " + format, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 创建文件过大异常
     * 
     * @param maxSize 最大文件大小（字节）
     * @return BusinessException
     */
    public static BusinessException fileTooLarge(long maxSize) {
        long maxSizeMB = maxSize / (1024 * 1024);
        return new BusinessException(413, "文件大小超过限制，最大允许 " + maxSizeMB + "MB", HttpStatus.PAYLOAD_TOO_LARGE);
    }
    
    /**
     * 创建资源不存在异常
     * 
     * @param resourceName 资源名称
     * @param id 资源 ID
     * @return BusinessException
     */
    public static BusinessException resourceNotFound(String resourceName, Object id) {
        return new BusinessException(404, resourceName + " 不存在: " + id, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 创建外部服务调用失败异常
     * 
     * @param serviceName 服务名称
     * @param message 错误消息
     * @return BusinessException
     */
    public static BusinessException externalServiceError(String serviceName, String message) {
        return new BusinessException(502, serviceName + " 调用失败: " + message, HttpStatus.BAD_GATEWAY);
    }
    
    /**
     * 创建服务不可用异常
     * 
     * @param message 错误消息
     * @return BusinessException
     */
    public static BusinessException serviceUnavailable(String message) {
        return new BusinessException(503, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
