package com.dbass.oms.api.controller;

import com.dbass.oms.api.dto.WebLoginRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("웹 로그인/로그아웃 API 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 로그인 후 토큰을 추출하는 헬퍼 메서드
    private String loginAndGetToken(String userId, String password) throws Exception {
        WebLoginRequestDto request = new WebLoginRequestDto();
        request.setUserId(userId);
        request.setUserPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    // ──────────────────────────────────────────
    // 로그인 테스트
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
    // 메시지 조회 테스트 (type=2 접근 권한)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/messages/search (웹 사용자 접근)")
    class MessageSearch {

        @Test
        @DisplayName("성공 - 웹 사용자(type=2) 토큰으로 메시지 조회")
        void success_withWebToken() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            String body = """
                    {
                        "startDate": "20250101",
                        "endDate": "20251231"
                    }
                    """;

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("실패 - 토큰 없이 메시지 조회")
        void fail_noToken() throws Exception {
            String body = """
                    {
                        "startDate": "20250101",
                        "endDate": "20251231"
                    }
                    """;

            mockMvc.perform(post("/api/messages/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 토큰")
        void fail_invalidToken() throws Exception {
            String body = """
                    {
                        "startDate": "20250101",
                        "endDate": "20251231"
                    }
                    """;

            mockMvc.perform(post("/api/messages/search")
                            .header("Authorization", "Bearer invalid.token.value")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────
    // 로그아웃 테스트
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
        @DisplayName("실패 - 토큰 없이 로그아웃 시도")
        void fail_noToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}