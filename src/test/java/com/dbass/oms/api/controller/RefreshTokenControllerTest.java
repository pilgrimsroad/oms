package com.dbass.oms.api.controller;

import com.dbass.oms.api.config.TestConfig;
import com.dbass.oms.api.dto.WebLoginRequestDto;
import com.dbass.oms.api.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("Refresh Token API 테스트")
class RefreshTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0).equals(inv.getArgument(1)));
        lenient().when(passwordEncoder.encode(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private record TokenPair(String accessToken, String refreshToken) {}

    private TokenPair loginAndGetTokens(String userId, String password) throws Exception {
        WebLoginRequestDto request = new WebLoginRequestDto();
        request.setUserId(userId);
        request.setUserPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TokenPair(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText()
        );
    }

    private TokenPair issueApiTokenPair(String userId, String userUrl, String password) throws Exception {
        String body = String.format("""
                {
                    "userId": "%s",
                    "userUrl": "%s",
                    "userPassword": "%s"
                }
                """, userId, userUrl, password);

        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TokenPair(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText()
        );
    }

    // ──────────────────────────────────────────
    // 로그인/토큰 발급 시 Refresh Token 포함 여부
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("토큰 발급 응답에 refreshToken 포함")
    class TokenIssuance {

        @Test
        @DisplayName("로그인(login) 응답에 refreshToken 포함")
        void login_includesRefreshToken() throws Exception {
            TokenPair tokens = loginAndGetTokens("WEB_USER_01", "password1");

            assert tokens.refreshToken() != null && !tokens.refreshToken().isBlank()
                    : "refreshToken이 응답에 포함되어야 합니다.";
        }

        @Test
        @DisplayName("토큰 발급(token) 응답에 refreshToken 포함")
        void issueToken_includesRefreshToken() throws Exception {
            TokenPair tokens = issueApiTokenPair("DEMO_USER", "https://example.com", "password");

            assert tokens.refreshToken() != null && !tokens.refreshToken().isBlank()
                    : "refreshToken이 응답에 포함되어야 합니다.";
        }
    }

    // ──────────────────────────────────────────
    // POST /api/auth/refresh
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("성공 - 유효한 Refresh Token으로 새 토큰 재발급")
        void success() throws Exception {
            String refreshToken = "valid-refresh-token";
            when(tokenBlacklistService.getRefreshTokenData(refreshToken))
                    .thenReturn(new String[]{"WEB_USER_01", "https://web.oms.local", "2"});

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "refreshToken": "valid-refresh-token"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(not(emptyString())))
                    .andExpect(jsonPath("$.refreshToken").value(not(emptyString())))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresInMinutes").isNumber());
        }

        @Test
        @DisplayName("성공 - Rotation: 재발급 후 기존 Refresh Token 삭제 호출")
        void success_rotation() throws Exception {
            String refreshToken = "old-refresh-token";
            when(tokenBlacklistService.getRefreshTokenData(refreshToken))
                    .thenReturn(new String[]{"WEB_USER_01", "https://web.oms.local", "2"});

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    { "refreshToken": "%s" }
                                    """, refreshToken)))
                    .andExpect(status().isOk());

            // 기존 Refresh Token이 삭제되었는지 확인 (Rotation 전략)
            verify(tokenBlacklistService, times(1)).deleteRefreshToken(refreshToken);
            // 새 Refresh Token이 저장되었는지 확인
            verify(tokenBlacklistService, times(1))
                    .saveRefreshToken(anyString(), anyString(), anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 Refresh Token → 401")
        void fail_invalidRefreshToken() throws Exception {
            when(tokenBlacklistService.getRefreshTokenData("invalid-token"))
                    .thenReturn(null);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "refreshToken": "invalid-token"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("유효하지 않거나 만료된 Refresh Token입니다."));
        }

        @Test
        @DisplayName("실패 - refreshToken 필드 누락 → 400")
        void fail_missingRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 빈 문자열 → 400")
        void fail_emptyRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "refreshToken": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // 로그아웃 시 Refresh Token 삭제
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("로그아웃 시 Refresh Token 삭제")
    class LogoutWithRefreshToken {

        @Test
        @DisplayName("성공 - refreshToken 포함 로그아웃 시 삭제 호출")
        void success_deleteRefreshTokenOnLogout() throws Exception {
            TokenPair tokens = loginAndGetTokens("WEB_USER_01", "password1");
            String refreshToken = tokens.refreshToken();

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    { "refreshToken": "%s" }
                                    """, refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));

            verify(tokenBlacklistService, times(1)).deleteRefreshToken(refreshToken);
        }

        @Test
        @DisplayName("성공 - refreshToken 없이 로그아웃도 정상 처리")
        void success_logoutWithoutRefreshToken() throws Exception {
            TokenPair tokens = loginAndGetTokens("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."));
        }
    }
}
