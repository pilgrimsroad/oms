package com.dbass.oms.api.repository;

import com.dbass.oms.api.config.QueryDslConfig;
import com.dbass.oms.api.entity.MessageLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("MessageRepository QueryDSL 필터/페이징 테스트")
class MessageRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        persist("20250101120000", 1, 2, "01011111111"); // SMS, 성공
        persist("20250201120000", 2, 9, "01022222222"); // LMS, 실패
        persist("20250301120000", 1, 2, "01033333333"); // SMS, 성공
        persist("20260101120000", 1, 2, "01044444444"); // 다른 연도 (2026)
        em.flush();
        em.clear();
    }

    private void persist(String submitTime, int msgType, int status, String rcptData) {
        MessageLog log = new MessageLog();
        log.setSubject("제목");
        log.setMessage("내용");
        log.setMsgType(msgType);
        log.setStatus(status);
        log.setSubmitTime(submitTime);
        log.setRcptData(rcptData);
        log.setCallbackNum("01000000000");
        em.persist(log);
    }

    private PageRequest page(int size) {
        return PageRequest.of(0, size);
    }

    // ──────────────────────────────────────────
    // 날짜 범위 필터
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("날짜 범위 필터")
    class DateRange {

        @Test
        @DisplayName("2025년 범위 조회 - 3건")
        void inRange() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("2026년 범위 조회 - 1건")
        void anotherYear() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20260101000000", "20261231235959", null, null, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("해당 범위에 데이터 없으면 0건")
        void outOfRange() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20240101000000", "20241231235959", null, null, null, page(10));
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────
    // 단일 조건 필터
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("단일 조건 필터")
    class SingleFilter {

        @Test
        @DisplayName("status=2(성공) 필터 - 2건")
        void byStatus() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", 2, null, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(m -> m.getStatus() == 2);
        }

        @Test
        @DisplayName("status=9(실패) 필터 - 1건")
        void byStatusFailed() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", 9, null, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(9);
        }

        @Test
        @DisplayName("msgType=1(SMS) 필터 - 2건")
        void byMsgType() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, 1, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(m -> m.getMsgType() == 1);
        }

        @Test
        @DisplayName("recipient 부분 일치 - 010222 → 1건")
        void byRecipient() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, "010222", page(10));
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getRcptData()).isEqualTo("01022222222");
        }

        @Test
        @DisplayName("recipient 전체 번호 일치 - 1건")
        void byRecipientExact() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, "01033333333", page(10));
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // ──────────────────────────────────────────
    // 복합 조건 필터
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("복합 조건 필터")
    class MultiFilter {

        @Test
        @DisplayName("msgType=1 + status=2 → 2건")
        void msgTypeAndStatus() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", 2, 1, null, page(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(m -> m.getMsgType() == 1 && m.getStatus() == 2);
        }

        @Test
        @DisplayName("msgType=1 + recipient='010111' → 1건")
        void msgTypeAndRecipient() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, 1, "010111", page(10));
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("조건에 맞는 데이터 없으면 0건")
        void noMatch() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", 99, null, null, page(10));
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ──────────────────────────────────────────
    // 페이징
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("페이징")
    class Paging {

        @Test
        @DisplayName("size=2 → content 2건, totalElements=3, totalPages=2")
        void pageSize() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, null, PageRequest.of(0, 2));
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("2페이지 조회 → content 1건")
        void secondPage() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, null, PageRequest.of(1, 2));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("submit_time 내림차순 정렬")
        void sortDesc() {
            Page<MessageLog> result = messageRepository.findByFilters(
                    "20250101000000", "20251231235959", null, null, null, page(10));
            List<String> times = result.getContent().stream()
                    .map(MessageLog::getSubmitTime)
                    .toList();
            assertThat(times).isSortedAccordingTo(Comparator.reverseOrder());
        }
    }
}