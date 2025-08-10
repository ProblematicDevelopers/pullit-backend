package com.pullit.common.cache.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface CacheService {

    /**
     * 캐시에서 값 조회
     * @param key 캐시 키
     * @param type 반환 타입 클래스
     * @return 캐시된 값, 없으면 null
     */
    <T> T get(String key, Class<T> type);

    /**
     * 캐시에 값 저장
     * @param key 캐시 키
     * @param value 저장할 값
     * @param ttl 유효 시간
     * @param timeUnit 시간 단위
     */
    void put(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 캐시에 값 저장 (기본 TTL 1시간)
     */
    void put(String key, Object value);

    /**
     * Cache-Aside 패턴 구현
     * 캐시에 없으면 Supplier로 값을 생성하여 캐시에 저장
     */
    <T> T getOrElse(String key, long ttl, TimeUnit timeUnit,
                    Supplier<T> valueSupplier, Class<T> type);

    /**
     * 캐시 삭제
     */
    void evict(String key);

    /**
     * 패턴과 일치하는 모든 캐시 삭제
     * @param pattern 키 패턴 (예: "user:*")
     */
    void evictByPattern(String pattern);

    /**
     * 캐시 키 존재 여부 확인
     */
    boolean exists(String key);

    /**
     * TTL 조회 (초 단위)
     * @return TTL, 만료되지 않으면 -1, 키가 없으면 -2
     */
    long getTtl(String key);

}
