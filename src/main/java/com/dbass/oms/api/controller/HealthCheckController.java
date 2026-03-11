package com.dbass.oms.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "DB Connection Check", description = "OMS DB서버 연결상태 확인용 API")
public class HealthCheckController {

    private final DataSource dataSource;

    @GetMapping("/health")
    @Operation(
        summary = "DB 연결 여부",
        description = "OMS DB서버의 정상 동작 여부를 확인합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "서버 정상 동작")
        }
    )
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(1);
            result.put("db", valid ? "OK" : "FAIL");
        } catch (SQLException e) {
            result.put("db", "FAIL: " + e.getMessage());
        }

        result.put("time", LocalDateTime.now().toString());
        result.put("status", "UP");

        return result;
    }
} 