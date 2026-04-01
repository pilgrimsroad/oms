package com.dbass.oms.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebLoginResponseDto {
    private String accessToken;
    private String tokenType;
    private long expiresInMinutes;
    private String userId;
    private String userType;
}