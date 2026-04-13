package com.dbass.oms.api.service;

import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.dto.PagedResponseDto;

public interface MessageService {
    PagedResponseDto<MessageResponseDto> searchMessages(MessageSearchRequestDto requestDto);
}
