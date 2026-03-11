package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 등록 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterResponseDto {

    @Schema(description = "등록요청 사용자 ID", example = "OMS")
    private String userId;

    @Schema(description = "등록요청 사용자 URL", example = "https://example.com")
    private String userUrl;

    @Schema(description = "사용자 타입", example = "1")
    private String userType;

    @Schema(description = "응답 메시지", example = "사용자가 성공적으로 등록되었습니다.")
    private String message;
} 