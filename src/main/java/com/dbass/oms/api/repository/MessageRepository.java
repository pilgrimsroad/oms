package com.dbass.oms.api.repository;

import com.dbass.oms.api.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageLog, Long> {
    List<MessageLog> findBySubmitTimeBetween(String startSubmitTime, String endSubmitTime);
}