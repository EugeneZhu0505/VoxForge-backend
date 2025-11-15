package com.eugenezhu.voxforge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.exception
 * @className: GlobalExceptionHandler
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午8:21
 */
@Slf4j
@RestControllerAdvice // 添加这个注解，表示这个类是异常处理器
public class GlobalExceptionHandler {

    /**
     * 处理认证异常
     * @param e
     * @return
     */
    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常: {}", e.getMessage());

        Map<String, Object>  errorResponse = new HashMap<>();
        errorResponse.put("error", "认证异常");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    /**
     * 处理参数验证异常
     * @param e
     * @return
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(WebExchangeBindException e) {
        log.error("参数验证异常: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "参数验证失败");
        errorResponse.put("message", "请检查输入参数");
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }


    /**
     * 处理运行时异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "服务异常");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    /**
     * 处理其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "系统异常");
        errorResponse.put("message", "服务暂时不可用，请稍后重试");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}

