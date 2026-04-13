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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("인증 API 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpPasswordEncoder() {
        // H2에 평문 비밀번호가 저장되어 있으므로 plain-text 비교로 동작
        lenient().when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0).equals(inv.getArgument(1)));
        lenient().when(passwordEncoder.encode(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
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
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    // ──────────────────────────────────────────
    // 회원 등록 (POST /api/auth/register)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("성공 - 신규 사용자 등록")
        void success() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "NEW_API_USER",
                                        "userUrl": "https://new-service.com",
                                        "userType": "1",
                                        "userPassword": "newpassword",
                                        "insertId": "admin"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("NEW_API_USER"))
                    .andExpect(jsonPath("$.userUrl").value("https://new-service.com"))
                    .andExpect(jsonPath("$.message").value("사용자가 성공적으로 등록되었습니다."));
        }

        @Test
        @DisplayName("실패 - 동일한 userId + userUrl 중복 등록")
        void fail_duplicate() throws Exception {
            // 첫 번째 등록
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "DUP_USER",
                                        "userUrl": "https://dup.com",
                                        "userType": "1",
                                        "userPassword": "password",
                                        "insertId": "admin"
                                    }
                                    """))
                    .andExpect(status().isOk());

            // 중복 등록 시도
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "DUP_USER",
                                        "userUrl": "https://dup.com",
                                        "userType": "1",
                                        "userPassword": "password",
                                        "insertId": "admin"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("이미 등록된")));
        }

        @Test
        @DisplayName("실패 - 필수 필드(userId) 누락")
        void fail_missingUserId() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userUrl": "https://example.com",
                                        "userPassword": "password",
                                        "insertId": "admin"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 필수 필드(insertId) 누락")
        void fail_missingInsertId() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "SOME_USER",
                                        "userUrl": "https://example.com",
                                        "userPassword": "password"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // JWT 토큰 발급 (POST /api/auth/token)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/auth/token")
    class IssueToken {

        @Test
        @DisplayName("성공 - DEMO_USER(type=1) 토큰 발급")
        void success() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "DEMO_USER",
                                        "userUrl": "https://example.com",
                                        "userPassword": "password"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호")
        void fail_wrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "DEMO_USER",
                                        "userUrl": "https://example.com",
                                        "userPassword": "wrongpassword"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("비밀번호")));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "NO_USER",
                                        "userUrl": "https://no-user.com",
                                        "userPassword": "password"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("찾을 수 없습니다")));
        }

        @Test
        @DisplayName("실패 - 필수 필드(userUrl) 누락")
        void fail_missingUserUrl() throws Exception {
            mockMvc.perform(post("/api/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "userId": "DEMO_USER",
                                        "userPassword": "password"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // 웹 로그인 (POST /api/auth/login)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("성공 - WEB_USER_01 정상 로그인")
        void success() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserId("WEB_USER_01");
            request.setUserPassword("password1");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print()) // 요청/응답 전체 콘솔 출력 — 확인 후 제거 가능
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.userId").value("WEB_USER_01"))
                    .andExpect(jsonPath("$.userType").value("2"));
        }

        @Test
        @DisplayName("성공 - WEB_USER_02 정상 로그인")
        void success_user02() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserId("WEB_USER_02");
            request.setUserPassword("password2");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("WEB_USER_02"))
                    .andExpect(jsonPath("$.userType").value("2"));
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호")
        void fail_wrongPassword() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserId("WEB_USER_01");
            request.setUserPassword("wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("비밀번호")));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserId("NO_SUCH_USER");
            request.setUserPassword("password1");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("찾을 수 없습니다")));
        }

        @Test
        @DisplayName("실패 - API 타입(type=1) 계정은 웹 로그인 불가")
        void fail_apiTypeUser() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserId("DEMO_USER");
            request.setUserPassword("password");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").isNotEmpty());
        }

        @Test
        @DisplayName("실패 - 요청 바디 누락 (userId 없음)")
        void fail_missingUserId() throws Exception {
            WebLoginRequestDto request = new WebLoginRequestDto();
            request.setUserPassword("password1");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // 로그아웃 (POST /api/auth/logout)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("성공 - 유효한 토큰으로 로그아웃")
        void success() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 되었습니다."))
                    .andExpect(jsonPath("$.userId").value("WEB_USER_01"));
        }

        @Test
        @DisplayName("실패 - 토큰 없이 로그아웃 시도 → 401")
        void fail_noToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}