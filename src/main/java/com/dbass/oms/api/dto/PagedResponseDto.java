package com.dbass.oms.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이지네이션 응답 래퍼 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponseDto<T> {

    @Schema(description = "조회 결과 목록")
    private List<T> content;

    @Schema(description = "전체 건수", example = "50000")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "500")
    private int totalPages;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int currentPage;

    @Schema(description = "페이지 크기", example = "100")
    private int pageSize;
}
