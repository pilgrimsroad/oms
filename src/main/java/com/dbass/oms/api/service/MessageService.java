package com.dbass.oms.api.service;

import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;

import java.util.List;

public interface MessageService {
    List<MessageResponseDto> searchMessages(MessageSearchRequestDto requestDto);
} 