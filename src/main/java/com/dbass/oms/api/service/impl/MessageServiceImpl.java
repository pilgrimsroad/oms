package com.dbass.oms.api.service.impl;

import com.dbass.oms.api.config.CacheConfig;
import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.dto.PagedResponseDto;
import com.dbass.oms.api.entity.MessageLog;
import com.dbass.oms.api.repository.MessageRepository;
import com.dbass.oms.api.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    @Cacheable(value = CacheConfig.MESSAGES_CACHE,
               key = "#requestDto.startDate + ':' + #requestDto.endDate + ':' + #requestDto.msgType + ':' + #requestDto.status + ':' + #requestDto.recipient + ':' + #requestDto.page + ':' + #requestDto.size")
    public PagedResponseDto<MessageResponseDto> searchMessages(MessageSearchRequestDto requestDto) {
        String start = requestDto.getStartDate() + "000000";
        String end   = requestDto.getEndDate()   + "235959";

        Integer msgType = parseIntOrNull(requestDto.getMsgType());
        String recipient = (requestDto.getRecipient() != null && !requestDto.getRecipient().isBlank())
                ? requestDto.getRecipient() : null;

        PageRequest pageable = PageRequest.of(
                requestDto.getPage(),
                requestDto.getSize(),
                Sort.by(Sort.Direction.DESC, "submitTime")
        );

        Page<MessageLog> page = messageRepository.findByFilters(
                start, end, requestDto.getStatus(), msgType, recipient, pageable
        );

        List<MessageResponseDto> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponseDto<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MessageResponseDto toResponse(MessageLog message) {
        String submitTime = message.getSubmitTime();
        String checkDate = (submitTime != null && submitTime.length() >= 8) ? submitTime.substring(0, 8) : null;
        return new MessageResponseDto(
                message.getMsgId(),
                message.getSubject(),
                message.getMessage(),
                Objects.toString(message.getMsgType(), null),
                resolveMsgTypeName(message.getMsgType()),
                Objects.toString(message.getStatus(), null),
                resolveStatusName(message.getStatus()),
                message.getScheduleTime(),
                submitTime,
                checkDate,
                message.getCallbackNum(),
                message.getRcptData(),
                message.getResult(),
                message.getResultDesc(),
                message.getExternalMessageId(),
                message.getRequestedAt(),
                message.getSentAt(),
                message.getErrorCode(),
                message.getRetryCount()
        );
    }

    private String resolveMsgTypeName(Integer msgType) {
        if (msgType == null) return "기타";
        return switch (msgType) {
            case 1 -> "SMS";
            case 2 -> "LMS";
            case 3 -> "MMS";
            case 6 -> "알림톡";
            case 7 -> "친구톡";
            case 8 -> "AI알림톡";
            case 9 -> "RCS SMS";
            case 10 -> "RCS LMS";
            case 11 -> "RCS MMS";
            case 12 -> "RCS 템플릿";
            case 13 -> "RCS 이미지템플릿";
            default -> "기타";
        };
    }

    private String resolveStatusName(Integer status) {
        if (status == null) return "기타";
        return switch (status) {
            case 0 -> "전송대기";
            case 1, 3, 5, 7 -> "전송중";
            case 2, 4, 6, 8 -> "전송완료";
            case 9 -> "실패";
            default -> "기타";
        };
    }
}
