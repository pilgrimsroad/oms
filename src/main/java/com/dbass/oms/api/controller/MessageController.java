package com.dbass.oms.api.controller;

import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.exception.InvalidRequestException;
import com.dbass.oms.api.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Message History", description = "문자 발송 이력 조회 API")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "문자 발송 이력 조회", description = "조회일자를 포함한 조건으로 문자 이력을 조회합니다.")
    @PostMapping("/search")
    public List<MessageResponseDto> searchMessages(@Valid @RequestBody MessageSearchRequestDto requestDto) {
        
        log.info("=== Message search request received ===");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication: {}", auth != null ? auth.getPrincipal() : "null");
        log.info("Request: {}", requestDto);

        // ✅ 시작일이 종료일보다 늦으면 예외 발생
        if (requestDto.getStartDate().compareTo(requestDto.getEndDate()) > 0) {
            throw new InvalidRequestException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<MessageResponseDto> result = messageService.searchMessages(requestDto);
        log.info("Search completed. Result count: {}", result.size());
        return result;
    }
} 