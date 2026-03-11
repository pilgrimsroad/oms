package com.dbass.oms.api.exception;

import com.dbass.oms.api.dto.ApiErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidation(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error on {}: {}", request.getRequestURI(), validationErrors);
        
        ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                .error("입력값 검증 오류: " + validationErrors)
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponseDto> handleInvalidRequest(
            InvalidRequestException ex, 
            HttpServletRequest request) {
        
        log.warn("Invalid request on {}: {}", request.getRequestURI(), ex.getMessage());
        
        ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                .error(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponseDto> handleRuntimeException(
            RuntimeException ex, 
            HttpServletRequest request) {
        
        log.error("Runtime exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                .error(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleGeneral(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Unexpected exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                .error("서버 내부 오류가 발생했습니다.")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
} 