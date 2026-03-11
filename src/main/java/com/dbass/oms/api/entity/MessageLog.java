package com.dbass.oms.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "TEST_SUBMIT_LOG")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "msg_id")
    private Long msgId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message")
    private String message;

    @Column(name = "msg_type")
    private Integer msgType;

    @Column(name = "status")
    private Integer status;

    @Column(name = "schedule_time")
    private String scheduleTime;

    @Column(name = "submit_time")
    private String submitTime;

    @Column(name = "callback_num")
    private String callbackNum;

    @Column(name = "rcpt_data")
    private String rcptData;

    @Column(name = "result")
    private String result;

    @Column(name = "result_desc")
    private String resultDesc;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "requested_at")
    private String requestedAt;

    @Column(name = "sent_at")
    private String sentAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "retry_count")
    private Integer retryCount;
}
