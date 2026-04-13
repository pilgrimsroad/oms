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
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("메시지 조회 API 테스트")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0).equals(inv.getArgument(1)));
        lenient().when(passwordEncoder.encode(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(cacheManager.getCache(anyString()))
                .thenReturn(new ConcurrentMapCache("messages"));
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────
    private String loginAndGetToken(String userId, String password) throws Exception {
        WebLoginRequestDto request = new WebLoginRequestDto();
        request.setUserId(userId);
        request.setUserPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String issueApiToken(String userId, String userUrl, String password) throws Exception {
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
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    // ──────────────────────────────────────────
    // 성공 케이스
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("날짜만으로 조회 - 응답 구조 검증")
        void success_dateOnly() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.totalPages").isNumber())
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(100));
        }

        @Test
        @DisplayName("status + msgType + recipient 전체 필터 조회")
        void success_allFilters() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231",
                                        "status": 2,
                                        "msgType": "1",
                                        "recipient": "010"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("type=1(API) 토큰으로 메시지 조회 성공")
        void success_withApiTypeToken() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("페이지네이션 - page=0, size=5 → content 최대 5건")
        void success_paging() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231",
                                        "page": 0,
                                        "size": 5
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.currentPage").value(0));
        }
    }

    // ──────────────────────────────────────────
    // 인증 실패 케이스
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("인증 실패")
    class AuthFail {

        @Test
        @DisplayName("토큰 없이 요청 → 401")
        void fail_noToken() throws Exception {
            mockMvc.perform(post("/api/messages/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("유효하지 않은 토큰 → 401")
        void fail_invalidToken() throws Exception {
            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer invalid.token.value")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그아웃된(블랙리스트) 토큰 → 401")
        void fail_blacklistedToken() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");
            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20250101",
                                        "endDate": "20251231"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("로그아웃된 토큰입니다. 다시 로그인해 주세요."));
        }
    }

    // ──────────────────────────────────────────
    // 입력값 검증 실패
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("입력값 검증 실패")
    class ValidationFail {

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 400")
        void fail_startAfterEnd() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "startDate": "20251231",
                                        "endDate": "20250101"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}