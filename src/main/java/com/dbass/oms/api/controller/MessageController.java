package com.dbass.oms.api.controller;

import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.dto.MessageSendRequestDto;
import com.dbass.oms.api.dto.MessageSendResponseDto;
import com.dbass.oms.api.dto.PagedResponseDto;
import com.dbass.oms.api.exception.InvalidRequestException;
import com.dbass.oms.api.security.UserPrincipal;
import com.dbass.oms.api.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Message History", description = "문자 발송 이력 조회 API")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "문자 발송 이력 조회", description = "조회일자를 포함한 조건으로 문자 이력을 조회합니다.")
    @PostMapping("/search")
    public PagedResponseDto<MessageResponseDto> searchMessages(@Valid @RequestBody MessageSearchRequestDto requestDto) {

        log.info("=== Message search request received ===");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication: {}", auth != null ? auth.getPrincipal() : "null");
        log.info("Request: {}", requestDto);

        if (requestDto.getStartDate().compareTo(requestDto.getEndDate()) > 0) {
            throw new InvalidRequestException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        PagedResponseDto<MessageResponseDto> result = messageService.searchMessages(requestDto);
        log.info("Search completed. totalElements: {}, page: {}/{}", result.getTotalElements(), result.getCurrentPage() + 1, result.getTotalPages());
        return result;
    }

    @Operation(summary = "문자 발송 요청", description = "메시지 발송을 요청합니다. API 사용자(type=1)만 사용 가능합니다.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/send")
    public ResponseEntity<MessageSendResponseDto> sendMessage(
            @Valid @RequestBody MessageSendRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal principal) {

        MessageSendResponseDto response = messageService.send(requestDto, principal.getUserId());
        return ResponseEntity.ok(response);
    }
} 