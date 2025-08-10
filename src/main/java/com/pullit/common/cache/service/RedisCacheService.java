package com.pullit.common.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.pullit.common.constants.CacheConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 캐시에서 값 조회
     * 캐시 히트/미스 로깅
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                // CacheConstants의 메시지 사용
                log.debug(MSG_CACHE_MISS, key);
                return null;
            }

            // CacheConstants의 메시지 사용
            log.debug(MSG_CACHE_HIT, key);

            // 타입 체크 후 캐스팅
            if (type.isInstance(value)) {
                return (T) value;
            } else {
                log.warn("캐시 타입 불일치: key={}, expected={}, actual={}",
                        key, type.getName(), value.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            // CacheConstants의 메시지 사용
            log.error(MSG_CACHE_ERROR, key, e);
            return null;  // 캐시 실패 시 null 반환 (fallback)
        }
    }

    /**
     * 캐시에 값 저장
     * TTL과 함께 저장
     */
    @Override
    public void put(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            if (value == null) {
                log.warn("null 값은 캐시하지 않음: key={}", key);
                return;
            }

            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            // CacheConstants의 메시지 사용
            log.debug(MSG_CACHE_SAVED, key, ttl, timeUnit);

        } catch (Exception e) {
            // CacheConstants의 메시지 사용
            log.error(MSG_CACHE_ERROR, key, e);
            // 캐시 실패가 비즈니스 로직에 영향을 주지 않도록 예외를 전파하지 않음
        }
    }

    /**
     * 기본 TTL로 캐시 저장
     * CacheConstants의 DEFAULT_TTL_SECONDS 사용
     */
    @Override
    public void put(String key, Object value) {
        // CacheConstants의 기본 TTL 사용
        put(key, value, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Cache-Aside 패턴
     * 캐시에 없으면 Supplier로 값을 생성하여 캐시에 저장
     */
    @Override
    public <T> T getOrElse(String key, long ttl, TimeUnit timeUnit,
                           Supplier<T> valueSupplier, Class<T> type) {
        // 1. 캐시에서 먼저 조회
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }

        // 2. 캐시에 없으면 값 생성
        log.debug("캐시 미스로 인한 값 생성: key={}", key);
        T value = valueSupplier.get();

        // 3. 생성된 값을 캐시에 저장
        if (value != null) {
            put(key, value, ttl, timeUnit);
        }

        return value;
    }

    /**
     * 캐시 삭제
     */
    @Override
    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            // CacheConstants의 메시지 사용
            log.debug(MSG_CACHE_DELETED, key, deleted);
        } catch (Exception e) {
            // CacheConstants의 메시지 사용
            log.error(MSG_CACHE_ERROR, key, e);
        }
    }

    /**
     * 패턴과 일치하는 모든 캐시 삭제
     * 와일드카드(*) 패턴 지원
     */
    @Override
    public void evictByPattern(String pattern) {
        try {
            // SCAN을 사용하여 패턴과 일치하는 키 조회
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.debug("패턴 캐시 삭제: pattern={}, count={}", pattern, deleted);
            }
        } catch (Exception e) {
            log.error("패턴 캐시 삭제 실패: pattern={}", pattern, e);
        }
    }

    /**
     * 캐시 키 존재 여부 확인
     */
    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("캐시 존재 확인 실패: key={}", key, e);
            return false;
        }
    }

    /**
     * TTL 조회 (초 단위)
     * -1: 만료되지 않음
     * -2: 키가 존재하지 않음
     */
    @Override
    public long getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            log.error("TTL 조회 실패: key={}", key, e);
            return -2;
        }
    }

    /**
     * 캐시 키 생성 헬퍼 메소드
     * CacheConstants의 프리픽스와 구분자 사용
     */
    public String createUserCacheKey(Long userId) {
        return KEY_PREFIX_USER + userId;
    }

    public String createProductCacheKey(Long productId) {
        return KEY_PREFIX_PRODUCT + productId;
    }

    public String createSessionCacheKey(String sessionId) {
        return KEY_PREFIX_SESSION + sessionId;
    }

    public String createTokenCacheKey(String token) {
        return KEY_PREFIX_TOKEN + token;
    }

    /**
     * 복합 키 생성
     * 예: "user:123:profile"
     */
    public String createCompositeKey(String prefix, Object... parts) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object part : parts) {
            keyBuilder.append(KEY_DELIMITER).append(part);
        }
        return keyBuilder.toString();
    }

}
