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
            String token = jwtTokenProvider.generateToken(omsUser);

            WebLoginResponseDto response = WebLoginResponseDto.builder()
                    .accessToken(token)
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
        description = "JWT는 stateless이므로 서버에서 토큰을 무효화하지 않습니다. 클라이언트에서 토큰을 삭제하세요.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
        }
    )
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserPrincipal principal,
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
        return ResponseEntity.ok().body(java.util.Map.of(
                "message", "로그아웃 되었습니다.",
                "userId", principal.getUserId()
        ));
    }
} 