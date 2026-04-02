package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문자 발송 이력 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {

    @Schema(description = "메시지 ID", example = "1")
    private Long msgId;

    @Schema(description = "메시지 제목", example = "SMS 배송 안내")
    private String subject;

    @Schema(description = "메시지 본문", example = "배송이 시작되었습니다. 운송장 확인 부탁드립니다.")
    private String message;

    @Schema(description = "메시지 유형 코드", example = "1")
    private String msgType;

    @Schema(description = "메시지 유형명", example = "SMS")
    private String msgTypeNm;

    @Schema(description = "전송 상태 코드", example = "2")
    private String status;

    @Schema(description = "전송 상태명", example = "전송완료")
    private String statusNm;

    @Schema(description = "예약 시간 (YYYYMMDDHHMMSS)", example = "20250105103000")
    private String scheduleTime;

    @Schema(description = "등록 시간 (YYYYMMDDHHMMSS)", example = "20250105103010")
    private String submitTime;

    @Schema(description = "조회 기준일 (YYYYMMDD)", example = "20250105")
    private String checkDate;

    @Schema(description = "발신 번호", example = "01000000000")
    private String callbackNum;

    @Schema(description = "수신 번호", example = "01000000000")
    private String rcptData;

    @Schema(description = "결과 코드", example = "SUCCESS")
    private String result;

    @Schema(description = "결과 설명", example = "Delivered")
    private String resultDesc;

    @Schema(description = "외부 메시지 ID", example = "EXT-2025-0001")
    private String externalMessageId;

    @Schema(description = "요청 시간 (YYYYMMDDHHMMSS)", example = "20250105102930")
    private String requestedAt;

    @Schema(description = "발송 완료 시간 (YYYYMMDDHHMMSS)", example = "20250105103015")
    private String sentAt;

    @Schema(description = "실패 코드", example = "E500")
    private String errorCode;

    @Schema(description = "재시도 횟수", example = "0")
    private Integer retryCount;
} 