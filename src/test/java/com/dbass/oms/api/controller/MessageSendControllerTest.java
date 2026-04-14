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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("메시지 발송 API 테스트")
class MessageSendControllerTest {

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

    // ──────────────────────────────────────────
    // POST /api/messages/send
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/messages/send")
    class SendMessage {

        @Test
        @DisplayName("성공 - SMS 발송 요청 접수 (status=0 반환)")
        void success_sms() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "msgType": 1,
                                        "callbackNum": "01000000000",
                                        "rcptData": "01012345678",
                                        "message": "테스트 SMS 발송입니다."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msgId").isNumber())
                    .andExpect(jsonPath("$.msgType").value(1))
                    .andExpect(jsonPath("$.rcptData").value("01012345678"))
                    .andExpect(jsonPath("$.status").value(0))
                    .andExpect(jsonPath("$.requestedAt").isNotEmpty())
                    .andExpect(jsonPath("$.message").value("발송 요청이 접수되었습니다."));
        }

        @Test
        @DisplayName("성공 - 예약 발송 시간 포함")
        void success_withScheduleTime() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "msgType": 2,
                                        "callbackNum": "01000000000",
                                        "rcptData": "01099999999",
                                        "subject": "공지 안내",
                                        "message": "LMS 테스트 발송입니다.",
                                        "scheduleTime": "20260501120000"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.msgType").value(2))
                    .andExpect(jsonPath("$.status").value(0));
        }

        @Test
        @DisplayName("실패 - 토큰 없음 → 401")
        void fail_noToken() throws Exception {
            mockMvc.perform(post("/api/messages/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "msgType": 1,
                                        "callbackNum": "01000000000",
                                        "rcptData": "01012345678",
                                        "message": "테스트입니다."
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - msgType 누락 → 400")
        void fail_missingMsgType() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "callbackNum": "01000000000",
                                        "rcptData": "01012345678",
                                        "message": "테스트입니다."
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - callbackNum 누락 → 400")
        void fail_missingCallbackNum() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "msgType": 1,
                                        "rcptData": "01012345678",
                                        "message": "테스트입니다."
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - message 누락 → 400")
        void fail_missingMessage() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            mockMvc.perform(post("/api/messages/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "msgType": 1,
                                        "callbackNum": "01000000000",
                                        "rcptData": "01012345678"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // POST /api/agent/process
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/agent/process")
    class AgentProcess {

        @Test
        @DisplayName("성공 - 대기 건 처리 후 처리 건수 반환")
        void success_processedCount() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            // 발송 요청 2건 등록
            for (int i = 0; i < 2; i++) {
                mockMvc.perform(post("/api/messages/send")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "msgType": 1,
                                            "callbackNum": "01000000000",
                                            "rcptData": "01011112222",
                                            "message": "에이전트 처리 테스트"
                                        }
                                        """))
                        .andExpect(status().isOk());
            }

            // 에이전트 처리
            mockMvc.perform(post("/api/agent/process")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.processedCount").value(greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("성공 - 대기 건 없으면 0 반환")
        void success_noPending() throws Exception {
            String token = issueApiToken("DEMO_USER", "https://example.com", "password");

            // 먼저 기존 대기 건 모두 처리
            mockMvc.perform(post("/api/agent/process")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // 다시 처리 요청 → 0건
            mockMvc.perform(post("/api/agent/process")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.processedCount").value(0))
                    .andExpect(jsonPath("$.message").value("0건 처리 완료"));
        }

        @Test
        @DisplayName("실패 - 토큰 없음 → 401")
        void fail_noToken() throws Exception {
            mockMvc.perform(post("/api/agent/process"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - type=2(Web) 유저는 접근 불가 → 403")
        void fail_webUserForbidden() throws Exception {
            String token = loginAndGetToken("WEB_USER_01", "password1");

            mockMvc.perform(post("/api/agent/process")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }
}
