package com.dbass.oms.api.repository;

import com.dbass.oms.api.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageLog, Long>, MessageRepositoryCustom {
}
