package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 문자 발송 이력 검색 요청 DTO
 */
@Data
public class MessageSearchRequestDto {

    @Schema(description = "조회 시작일자 (yyyyMMdd)", example = "20250612", required = true)
    private String startDate;

    @Schema(description = "조회 종료일자 (yyyyMMdd)", example = "20250613", required = true)
    private String endDate;

    @Schema(description = "전송 상태 코드 (선택)", example = "2")
    private Integer status;

    @Schema(description = "메시지 유형 코드 (선택, 예: 1=SMS)", example = "1")
    private String msgType;

    @Schema(description = "수신자 전화번호 일부 또는 전체", example = "010")
    private String recipient;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "100")
    private int size = 100;
} 