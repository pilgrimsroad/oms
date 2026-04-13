package com.dbass.oms.api.service;

import com.dbass.oms.api.dto.MessageResponseDto;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.dto.PagedResponseDto;
import com.dbass.oms.api.entity.MessageLog;
import com.dbass.oms.api.repository.MessageRepository;
import com.dbass.oms.api.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl 단위 테스트")
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────
    private MessageSearchRequestDto request(String start, String end) {
        MessageSearchRequestDto req = new MessageSearchRequestDto();
        req.setStartDate(start);
        req.setEndDate(end);
        return req;
    }

    private MessageLog logEntry(int msgType, int status, String submitTime) {
        MessageLog log = new MessageLog();
        log.setMsgId(1L);
        log.setMsgType(msgType);
        log.setStatus(status);
        log.setSubmitTime(submitTime);
        log.setRcptData("01011111111");
        log.setSubject("제목");
        return log;
    }

    private Page<MessageLog> pageOf(MessageLog... logs) {
        return new PageImpl<>(List.of(logs), PageRequest.of(0, 100), logs.length);
    }

    // ──────────────────────────────────────────
    // 날짜 포맷 변환
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("날짜 포맷 변환")
    class DateFormat {

        @Test
        @DisplayName("startDate → 000000, endDate → 235959 붙여서 Repository에 전달")
        void appendTimeToDate() {
            when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            messageService.searchMessages(request("20250101", "20250131"));

            verify(messageRepository).findByFilters(
                    eq("20250101000000"), eq("20250131235959"),
                    isNull(), isNull(), isNull(), any()
            );
        }
    }

    // ──────────────────────────────────────────
    // msgType 파싱
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("msgType 파싱")
    class MsgTypeParsing {

        @Test
        @DisplayName("'1' → Integer 1로 전달")
        void valid() {
            when(messageRepository.findByFilters(any(), any(), any(), eq(1), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setMsgType("1");
            messageService.searchMessages(req);

            verify(messageRepository).findByFilters(any(), any(), any(), eq(1), any(), any());
        }

        @Test
        @DisplayName("빈 문자열 → null로 전달")
        void blank() {
            when(messageRepository.findByFilters(any(), any(), any(), isNull(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setMsgType("");
            messageService.searchMessages(req);

            verify(messageRepository).findByFilters(any(), any(), any(), isNull(), any(), any());
        }

        @Test
        @DisplayName("숫자가 아닌 문자열 → null로 전달")
        void nonNumeric() {
            when(messageRepository.findByFilters(any(), any(), any(), isNull(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setMsgType("abc");
            messageService.searchMessages(req);

            verify(messageRepository).findByFilters(any(), any(), any(), isNull(), any(), any());
        }
    }

    // ──────────────────────────────────────────
    // recipient 처리
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("recipient 처리")
    class Recipient {

        @Test
        @DisplayName("공백만 있는 문자열 → null로 전달")
        void blank() {
            when(messageRepository.findByFilters(any(), any(), any(), any(), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setRecipient("   ");
            messageService.searchMessages(req);

            verify(messageRepository).findByFilters(any(), any(), any(), any(), isNull(), any());
        }

        @Test
        @DisplayName("값이 있으면 그대로 전달")
        void present() {
            when(messageRepository.findByFilters(any(), any(), any(), any(), eq("010"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setRecipient("010");
            messageService.searchMessages(req);

            verify(messageRepository).findByFilters(any(), any(), any(), any(), eq("010"), any());
        }
    }

    // ──────────────────────────────────────────
    // msgTypeNm 변환
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("메시지 유형명(msgTypeNm) 변환")
    class MsgTypeNm {

        private String getMsgTypeNm(int code) {
            when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(pageOf(logEntry(code, 2, "20250101120000")));
            PagedResponseDto<MessageResponseDto> result = messageService.searchMessages(request("20250101", "20250131"));
            return result.getContent().get(0).getMsgTypeNm();
        }

        @Test @DisplayName("1 → SMS")       void sms()        { assertThat(getMsgTypeNm(1)).isEqualTo("SMS"); }
        @Test @DisplayName("2 → LMS")       void lms()        { assertThat(getMsgTypeNm(2)).isEqualTo("LMS"); }
        @Test @DisplayName("3 → MMS")       void mms()        { assertThat(getMsgTypeNm(3)).isEqualTo("MMS"); }
        @Test @DisplayName("6 → 알림톡")    void alimtalk()   { assertThat(getMsgTypeNm(6)).isEqualTo("알림톡"); }
        @Test @DisplayName("7 → 친구톡")    void friendtalk() { assertThat(getMsgTypeNm(7)).isEqualTo("친구톡"); }
        @Test @DisplayName("9 → RCS SMS")   void rcsSms()     { assertThat(getMsgTypeNm(9)).isEqualTo("RCS SMS"); }
        @Test @DisplayName("99 → 기타")     void etc()        { assertThat(getMsgTypeNm(99)).isEqualTo("기타"); }
    }

    // ──────────────────────────────────────────
    // statusNm 변환
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("전송 상태명(statusNm) 변환")
    class StatusNm {

        private String getStatusNm(int code) {
            when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(pageOf(logEntry(1, code, "20250101120000")));
            PagedResponseDto<MessageResponseDto> result = messageService.searchMessages(request("20250101", "20250131"));
            return result.getContent().get(0).getStatusNm();
        }

        @Test @DisplayName("0 → 전송대기")  void waiting()  { assertThat(getStatusNm(0)).isEqualTo("전송대기"); }
        @Test @DisplayName("1 → 전송중")    void sending()  { assertThat(getStatusNm(1)).isEqualTo("전송중"); }
        @Test @DisplayName("2 → 전송완료")  void done()     { assertThat(getStatusNm(2)).isEqualTo("전송완료"); }
        @Test @DisplayName("9 → 실패")      void failed()   { assertThat(getStatusNm(9)).isEqualTo("실패"); }
        @Test @DisplayName("99 → 기타")     void etc()      { assertThat(getStatusNm(99)).isEqualTo("기타"); }
    }

    // ──────────────────────────────────────────
    // PagedResponseDto 구조
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("PagedResponseDto 필드 검증")
    class PagedResponse {

        @Test
        @DisplayName("totalElements, totalPages, currentPage, pageSize 정확히 반환")
        void fields() {
            Page<MessageLog> mockPage = new PageImpl<>(
                    List.of(logEntry(1, 2, "20250101120000")),
                    PageRequest.of(0, 10),
                    50
            );
            when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(mockPage);

            MessageSearchRequestDto req = request("20250101", "20250131");
            req.setPage(0);
            req.setSize(10);

            PagedResponseDto<MessageResponseDto> result = messageService.searchMessages(req);

            assertThat(result.getTotalElements()).isEqualTo(50);
            assertThat(result.getTotalPages()).isEqualTo(5);
            assertThat(result.getCurrentPage()).isEqualTo(0);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("결과 없을 때 빈 content, totalElements=0")
        void empty() {
            when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            PagedResponseDto<MessageResponseDto> result = messageService.searchMessages(request("20250101", "20250131"));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}