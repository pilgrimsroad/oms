package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 문자 발송 요청 DTO
 */
@Data
public class MessageSendRequestDto {

    @NotNull(message = "메시지 유형(msgType)은 필수입니다.")
    @Schema(description = "메시지 유형 코드 (1=SMS, 2=LMS, 3=MMS, 6=알림톡, 7=친구톡, 8=AI알림톡, 9~13=RCS)", example = "1")
    private Integer msgType;

    @NotBlank(message = "발신 번호(callbackNum)는 필수입니다.")
    @Schema(description = "발신 번호", example = "01000000000")
    private String callbackNum;

    @NotBlank(message = "수신 번호(rcptData)는 필수입니다.")
    @Schema(description = "수신 번호", example = "01012345678")
    private String rcptData;

    @Schema(description = "메시지 제목 (LMS/MMS 필요)", example = "배송 안내")
    private String subject;

    @NotBlank(message = "메시지 내용(message)은 필수입니다.")
    @Schema(description = "메시지 본문", example = "배송이 시작되었습니다.")
    private String message;

    @Schema(description = "예약 발송 시간 (yyyyMMddHHmmss, 미입력 시 즉시 발송)", example = "20250601120000")
    private String scheduleTime;
}
