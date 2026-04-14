package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageSendResponseDto {

    @Schema(description = "메시지 ID", example = "1")
    private Long msgId;

    @Schema(description = "메시지 유형", example = "1")
    private Integer msgType;

    @Schema(description = "수신 번호", example = "01012345678")
    private String rcptData;

    @Schema(description = "상태 (0=대기)", example = "0")
    private Integer status;

    @Schema(description = "요청 일시", example = "20250414153000")
    private String requestedAt;

    @Schema(description = "처리 메시지", example = "발송 요청이 접수되었습니다.")
    private String message;
}
