package com.pullit.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveSessionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "auth:session:";

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    public void setActiveSession(Long userId, String sessionId, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key(userId), sessionId, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Set active session for user {}: {} (ttl={}s)", userId, sessionId, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to set active session for user {}", userId, e);
        }
    }

    public String getActiveSession(Long userId) {
        try {
            return redisTemplate.opsForValue().get(key(userId));
        } catch (Exception e) {
            log.error("Failed to get active session for user {}", userId, e);
            return null;
        }
    }

    public boolean isActiveSession(Long userId, String sessionId) {
        if (sessionId == null) return false;
        String current = getActiveSession(userId);
        return sessionId.equals(current);
    }

    public void clearActiveSession(Long userId) {
        try {
            redisTemplate.delete(key(userId));
            log.debug("Cleared active session for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to clear active session for user {}", userId, e);
        }
    }

    public void clearIfMatch(Long userId, String sessionId) {
        try {
            String current = getActiveSession(userId);
            if (current != null && current.equals(sessionId)) {
                clearActiveSession(userId);
            }
        } catch (Exception e) {
            log.error("Failed to conditionally clear session for user {}", userId, e);
        }
    }

    public void extendIfMatch(Long userId, String sessionId, long ttlSeconds) {
        try {
            String current = getActiveSession(userId);
            if (current != null && current.equals(sessionId)) {
                // Re-set with same value to extend TTL
                setActiveSession(userId, sessionId, ttlSeconds);
            }
        } catch (Exception e) {
            log.error("Failed to extend session for user {}", userId, e);
        }
    }
}

