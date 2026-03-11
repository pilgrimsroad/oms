package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 등록 요청 DTO
 */
@Data
public class UserRegisterRequestDto {

    @Schema(description = "등록요청 사용자 ID", example = "TEST", required = true)
    @NotBlank(message = "등록요청 사용자 ID는 필수입니다")
    private String userId;

    @Schema(description = "등록요청 사용자 URL", example = "https://example.com", required = true)
    @NotBlank(message = "등록요청 사용자 URL은 필수입니다")
    private String userUrl;

    @Schema(description = "사용자 타입", example = "1: API, 2: 웹서비스")
    private String userType;

    @Schema(description = "로그인 비밀번호", example = "password", required = true)
    @NotBlank(message = "사용자 비밀번호는 필수입니다")
    private String userPassword;

    @Schema(description = "등록자 ID", example = "restAPI", required = true)
    @NotBlank(message = "등록자 ID는 필수입니다")
    private String insertId;
} 