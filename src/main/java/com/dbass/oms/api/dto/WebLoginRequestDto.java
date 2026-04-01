package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebLoginRequestDto {

    @Schema(description = "사용자 ID", example = "WEB_USER_01", required = true)
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @Schema(description = "비밀번호", example = "password1", required = true)
    @NotBlank(message = "비밀번호는 필수입니다")
    private String userPassword;
}