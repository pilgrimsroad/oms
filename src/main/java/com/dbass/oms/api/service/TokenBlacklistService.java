package com.dbass.oms.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String DELIMITER = "|";

    private final RedisTemplate<String, String> redisTemplate;

    // ── Access Token 블랙리스트 ──────────────────────────────

    public void blacklist(String token, Duration ttl) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // ── Refresh Token ────────────────────────────────────────

    public void saveRefreshToken(String token, String userId, String userUrl, String userType, Duration ttl) {
        String value = userId + DELIMITER + userUrl + DELIMITER + userType;
        redisTemplate.opsForValue().set(REFRESH_PREFIX + token, value, ttl);
    }

    public String[] getRefreshTokenData(String token) {
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + token);
        if (value == null) return null;
        return value.split("\\" + DELIMITER, -1);
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(REFRESH_PREFIX + token);
    }
}
