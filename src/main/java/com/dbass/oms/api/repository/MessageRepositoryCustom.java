package com.dbass.oms.api.repository;

import com.dbass.oms.api.entity.MessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageRepositoryCustom {

    Page<MessageLog> findByFilters(
            String start,
            String end,
            Integer status,
            Integer msgType,
            String recipient,
            Pageable pageable
    );
}
