package com.dbass.oms.api.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 테스트 공통 설정
 * - Redis → Mock 대체 (실제 Redis 서버 불필요)
 * - CacheManager는 각 테스트 클래스에서 @MockBean으로 처리
 * - PasswordEncoder는 각 테스트 클래스에서 @MockBean으로 처리
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}