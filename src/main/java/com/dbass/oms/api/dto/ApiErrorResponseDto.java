package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 에러 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponseDto {

    @Schema(description = "에러 메시지", example = "이미 등록된 사용자ID입니다.")
    private String error;

    @Schema(description = "에러 발생 시간", example = "2024-01-17T21:45:21")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "요청 경로", example = "/api/auth/register")
    private String path;

    @Schema(description = "디버깅 정보 (개발용)")
    private Map<String, Object> debug;
} 