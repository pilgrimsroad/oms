package com.dbass.oms.api.controller;

import com.dbass.oms.api.dto.*;
import com.dbass.oms.api.security.JwtTokenProvider;
import com.dbass.oms.api.security.UserPrincipal;
import com.dbass.oms.api.service.OmsUserService;
import com.dbass.oms.api.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "JWT Auth", description = "사용자 등록 및 JWT 발급")
@RequiredArgsConstructor
public class ApiKeyController {

    private final OmsUserService omsUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

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
            OmsUser omsUser = omsUserService.issueTokenUser(
                    authTokenRequestDto.getUserId(),
                    authTokenRequestDto.getUserUrl(),
                    authTokenRequestDto.getUserPassword()
            );
            String accessToken = jwtTokenProvider.generateToken(omsUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken();
            tokenBlacklistService.saveRefreshToken(
                    refreshToken,
                    omsUser.getUserId(),
                    omsUser.getUserUrl(),
                    omsUser.getUserType(),
                    java.time.Duration.ofMinutes(jwtTokenProvider.getRefreshExpirationMinutes())
            );

            AuthTokenResponseDto response = AuthTokenResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
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

    @PostMapping("/login")
    @Operation(
        summary = "웹 로그인",
        description = "웹 사용자(user_type=2) 전용 로그인. userId와 password만으로 JWT를 발급합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패")
        }
    )
    public ResponseEntity<?> loginWeb(
            @Valid @RequestBody WebLoginRequestDto request,
            HttpServletRequest httpRequest) {
        try {
            OmsUser omsUser = omsUserService.loginWeb(request.getUserId(), request.getUserPassword());
            String accessToken = jwtTokenProvider.generateToken(omsUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken();
            tokenBlacklistService.saveRefreshToken(
                    refreshToken,
                    omsUser.getUserId(),
                    omsUser.getUserUrl(),
                    omsUser.getUserType(),
                    java.time.Duration.ofMinutes(jwtTokenProvider.getRefreshExpirationMinutes())
            );

            WebLoginResponseDto response = WebLoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresInMinutes(jwtTokenProvider.getExpirationMinutes())
                    .userId(omsUser.getUserId())
                    .userType(omsUser.getUserType())
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

    @PostMapping("/logout")
    @Operation(
        summary = "로그아웃",
        description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
        }
    )
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RefreshTokenRequestDto logoutRequest,
            HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtTokenProvider.parseClaims(token);
            Date expiration = claims.getExpiration();
            long remainingSeconds = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
            if (remainingSeconds > 0) {
                tokenBlacklistService.blacklist(token, Duration.ofSeconds(remainingSeconds));
            }
        }
        if (logoutRequest != null && logoutRequest.getRefreshToken() != null) {
            tokenBlacklistService.deleteRefreshToken(logoutRequest.getRefreshToken());
        }
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "로그아웃 되었습니다.",
                "userId", principal.getUserId()
        ));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Access Token 재발급",
        description = "Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token")
        }
    )
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request,
            HttpServletRequest httpRequest) {
        String[] tokenData = tokenBlacklistService.getRefreshTokenData(request.getRefreshToken());
        if (tokenData == null) {
            ApiErrorResponseDto error = ApiErrorResponseDto.builder()
                    .error("유효하지 않거나 만료된 Refresh Token입니다.")
                    .path(httpRequest.getRequestURI())
                    .build();
            return ResponseEntity.status(401).body(error);
        }

        // Rotation: 기존 Refresh Token 즉시 폐기
        tokenBlacklistService.deleteRefreshToken(request.getRefreshToken());

        OmsUser omsUser = new OmsUser();
        omsUser.setUserId(tokenData[0]);
        omsUser.setUserUrl(tokenData[1]);
        omsUser.setUserType(tokenData[2]);

        String newAccessToken = jwtTokenProvider.generateToken(omsUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        tokenBlacklistService.saveRefreshToken(
                newRefreshToken,
                omsUser.getUserId(),
                omsUser.getUserUrl(),
                omsUser.getUserType(),
                java.time.Duration.ofMinutes(jwtTokenProvider.getRefreshExpirationMinutes())
        );

        AuthTokenResponseDto response = AuthTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMinutes(jwtTokenProvider.getExpirationMinutes())
                .build();

        return ResponseEntity.ok(response);
    }
} 