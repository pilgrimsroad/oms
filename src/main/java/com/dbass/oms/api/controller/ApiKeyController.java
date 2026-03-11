package com.dbass.oms.api.controller;

import com.dbass.oms.api.dto.*;
import com.dbass.oms.api.security.JwtTokenProvider;
import com.dbass.oms.api.service.OmsUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "JWT Auth", description = "사용자 등록 및 JWT 발급")
@RequiredArgsConstructor
public class ApiKeyController {

    private final OmsUserService omsUserService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @Operation(
        summary = "사용자 등록",
        description = "API 사용을 위한 사용자 정보를 등록합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
        }
    )
    public ResponseEntity<?> registerService(
            @Valid @RequestBody UserRegisterRequestDto userRegisterRequestDto,
            HttpServletRequest httpRequest) {
        try {
            OmsUser omsUser = omsUserService.registerService(
                userRegisterRequestDto.getUserId(),
                userRegisterRequestDto.getUserUrl(),
                userRegisterRequestDto.getUserType(),
                userRegisterRequestDto.getUserPassword(),
                userRegisterRequestDto.getInsertId()
            );
            
            UserRegisterResponseDto response = UserRegisterResponseDto.builder()
                    .userId(omsUser.getUserId())
                    .userUrl(omsUser.getUserUrl())
                    .userType(omsUser.getUserType())
                    .message("사용자가 성공적으로 등록되었습니다.")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                    .error(e.getMessage())
                    .path(httpRequest.getRequestURI())
                    .build();
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/token")
    @Operation(
        summary = "JWT 토큰 발급",
        description = "등록된 사용자 계정으로 JWT 토큰을 발급합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
        }
    )
    public ResponseEntity<?> issueToken(
            @Valid @RequestBody AuthTokenRequestDto authTokenRequestDto,
            HttpServletRequest httpRequest) {
        try {
            String token = omsUserService.issueToken(
                    authTokenRequestDto.getUserId(),
                    authTokenRequestDto.getUserUrl(),
                    authTokenRequestDto.getUserPassword()
            );

            AuthTokenResponseDto response = AuthTokenResponseDto.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresInMinutes(jwtTokenProvider.getExpirationMinutes())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                    .error(e.getMessage())
                    .path(httpRequest.getRequestURI())
                    .build();
            return ResponseEntity.badRequest().body(error);
        }
    }
} 