package com.example.rag.exception;

import com.example.rag.vo.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各种异常，返回友好的错误响应
 * 
 * 需求 9.1: 记录详细的错误日志（包含时间戳、错误类型、错误消息和堆栈跟踪）
 * 需求 9.2: 记录外部 API 调用失败的请求参数和响应信息
 * 需求 9.3: 返回用户友好的错误消息而不是技术细节
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        // 记录详细错误日志，包含异常类名和HTTP状态码
        if (e.getCause() != null) {
            log.warn("Business exception: type={}, code={}, httpStatus={}, message={}, cause={}",
                    e.getClass().getName(), e.getCode(), e.getHttpStatus(),
                    e.getMessage(), e.getCause().toString(), e);
        } else {
            log.warn("Business exception: type={}, code={}, httpStatus={}, message={}",
                    e.getClass().getName(), e.getCode(), e.getHttpStatus(), e.getMessage());
        }
        
        ApiResponse<Void> response = ApiResponse.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
    
    /**
     * 处理外部 API 调用响应异常（如 4xx、5xx 响应）
     * 记录请求 URL 和响应体，返回用户友好消息
     * 
     * @param e WebClient 响应异常
     * @return 错误响应
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientResponseException(WebClientResponseException e) {
        // 需求 9.1, 9.2: 记录外部 API 调用失败的详细信息
        log.error("External API call failed: type={}, statusCode={}, requestUrl={}, responseBody={}, message={}",
                e.getClass().getName(),
                e.getStatusCode().value(),
                e.getRequest() != null ? e.getRequest().getURI().toString() : "unknown",
                e.getResponseBodyAsString(),
                e.getMessage(), e);
        
        // 需求 9.3: 返回用户友好的错误消息，不暴露 API 细节
        ApiResponse<Void> response = ApiResponse.error(502, "外部服务暂时不可用，请稍后重试");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
    
    /**
     * 处理外部 API 连接异常（如连接超时、DNS 解析失败）
     * 记录连接失败详情，返回用户友好消息
     * 
     * @param e WebClient 请求异常
     * @return 错误响应
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientRequestException(WebClientRequestException e) {
        // 需求 9.1, 9.2: 记录连接失败的详细信息
        log.error("External API connection failed: type={}, requestUrl={}, method={}, message={}",
                e.getClass().getName(),
                e.getUri() != null ? e.getUri().toString() : "unknown",
                e.getMethod() != null ? e.getMethod().name() : "unknown",
                e.getMessage(), e);
        
        // 需求 9.3: 返回用户友好的错误消息
        ApiResponse<Void> response = ApiResponse.error(502, "外部服务连接失败，请稍后重试");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
    
    /**
     * 处理参数验证异常
     * 
     * @param e 参数验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("Validation exception: type={}, message={}", e.getClass().getName(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.error(400, "参数验证失败: " + errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理绑定异常
     * 
     * @param e 绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("Bind exception: type={}, message={}", e.getClass().getName(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.error(400, "参数绑定失败: " + errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理文件上传大小超限异常
     * 
     * @param e 文件上传大小超限异常
     * @return 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("File upload size exceeded: type={}, message={}", e.getClass().getName(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(413, "文件大小超过限制");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
    
    /**
     * 处理非法参数异常
     * 
     * @param e 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception: type={}, message={}", e.getClass().getName(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(400, "参数错误: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理空指针异常
     * 
     * @param e 空指针异常
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException e) {
        log.error("Null pointer exception: type={}, message={}", e.getClass().getName(), e.getMessage(), e);
        
        ApiResponse<Void> response = ApiResponse.error(500, "系统内部错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理运行时异常
     * 返回用户友好消息，不暴露技术细节
     * 
     * @param e 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        // 需求 9.1: 记录详细的错误日志
        log.error("Runtime exception occurred: type={}, message={}",
                e.getClass().getName(), e.getMessage(), e);
        
        // 需求 9.3: 返回用户友好的错误消息
        ApiResponse<Void> response = ApiResponse.error(500, "系统处理异常，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理所有未捕获的异常
     * 
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 需求 9.1: 记录详细的错误日志（包含时间戳、错误类型、错误消息和堆栈跟踪）
        log.error("Unexpected error occurred: type={}, message={}", 
                e.getClass().getName(), e.getMessage(), e);
        
        // 需求 9.3: 返回用户友好的错误消息（不包含技术细节）
        ApiResponse<Void> response = ApiResponse.error(500, "系统错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
