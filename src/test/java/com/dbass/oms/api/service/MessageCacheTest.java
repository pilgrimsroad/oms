package com.dbass.oms.api.service;

import com.dbass.oms.api.config.CacheConfig;
import com.dbass.oms.api.config.TestConfig;
import com.dbass.oms.api.dto.MessageSearchRequestDto;
import com.dbass.oms.api.entity.MessageLog;
import com.dbass.oms.api.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("메시지 캐시 동작 테스트")
class MessageCacheTest {

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private CacheManager cacheManager;

    @Autowired
    private MessageService messageService;

    private final ConcurrentMapCache testCache = new ConcurrentMapCache(CacheConfig.MESSAGES_CACHE);

    @BeforeEach
    void setUp() {
        testCache.clear();
        lenient().when(cacheManager.getCache(eq(CacheConfig.MESSAGES_CACHE))).thenReturn(testCache);

        when(messageRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new MessageLog())));
    }

    @Test
    @DisplayName("동일 조건 재조회 시 Repository는 1회만 호출 (캐시 히트)")
    void cacheHit_sameRequest() {
        MessageSearchRequestDto req = request("20250101", "20251231");

        messageService.searchMessages(req);
        messageService.searchMessages(req);

        verify(messageRepository, times(1))
                .findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 날짜 조건은 별도 캐시 키 → Repository 각각 호출")
    void cacheMiss_differentRequest() {
        messageService.searchMessages(request("20250101", "20250630"));
        messageService.searchMessages(request("20250701", "20251231"));

        verify(messageRepository, times(2))
                .findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("status 조건이 다르면 별도 캐시 키")
    void cacheMiss_differentStatus() {
        MessageSearchRequestDto req1 = request("20250101", "20251231");
        req1.setStatus(2);

        MessageSearchRequestDto req2 = request("20250101", "20251231");
        req2.setStatus(9);

        messageService.searchMessages(req1);
        messageService.searchMessages(req2);

        verify(messageRepository, times(2))
                .findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("page 조건이 다르면 별도 캐시 키")
    void cacheMiss_differentPage() {
        MessageSearchRequestDto req1 = request("20250101", "20251231");
        req1.setPage(0);

        MessageSearchRequestDto req2 = request("20250101", "20251231");
        req2.setPage(1);

        messageService.searchMessages(req1);
        messageService.searchMessages(req2);

        verify(messageRepository, times(2))
                .findByFilters(any(), any(), any(), any(), any(), any());
    }

    private MessageSearchRequestDto request(String start, String end) {
        MessageSearchRequestDto req = new MessageSearchRequestDto();
        req.setStartDate(start);
        req.setEndDate(end);
        return req;
    }
}