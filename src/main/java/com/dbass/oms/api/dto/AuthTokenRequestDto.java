package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * JWT 토큰 발급 요청 DTO
 */
@Data
public class AuthTokenRequestDto {

    @Schema(description = "사용자 ID", example = "DEMO_USER", required = true)
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @Schema(description = "사용자 URL", example = "https://example.com", required = true)
    @NotBlank(message = "사용자 URL은 필수입니다")
    private String userUrl;

    @Schema(description = "비밀번호", example = "password", required = true)
    @NotBlank(message = "비밀번호는 필수입니다")
    private String userPassword;
}
