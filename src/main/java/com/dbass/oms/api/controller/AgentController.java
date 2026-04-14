package com.dbass.oms.api.controller;

import com.dbass.oms.api.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로컬 전용 — 실제 운영에서는 외부 에이전트 서버가 이 역할을 수행합니다.
 * 발송 대기(status=0) 요청을 처리하고 TEST_SUBMIT_LOG에 결과를 기록합니다.
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent (Local Only)", description = "에이전트 서버 시뮬레이션 — 로컬 테스트 전용")
public class AgentController {

    private final MessageService messageService;

    @Operation(summary = "발송 대기 건수 조회", description = "현재 발송 대기(status=0) 건수를 반환합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Object>> pendingCount() {
        int count = messageService.getPendingCount();
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    @Operation(summary = "발송 대기 처리", description = "MSG_SEND_REQUEST의 대기(status=0) 건을 처리하고 TEST_SUBMIT_LOG에 결과를 기록합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process() {
        int processedCount = messageService.processAgentJob();
        return ResponseEntity.ok(Map.of(
                "processedCount", processedCount,
                "message", processedCount + "건 처리 완료"
        ));
    }
}
