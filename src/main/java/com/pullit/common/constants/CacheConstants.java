package com.pullit.common.constants;

public class CacheConstants {
    private CacheConstants() {}

    // 캐시 이름
    public static final String CACHE_USERS = "users";
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_SESSIONS = "sessions";
    public static final String CACHE_PERMISSIONS = "permissions";
    public static final String CACHE_CONFIGS = "configs";

    // TTL 설정 (초 단위)
    public static final long DEFAULT_TTL_SECONDS = 3600L;           // 1시간
    public static final long SHORT_TTL_SECONDS = 300L;              // 5분
    public static final long MEDIUM_TTL_SECONDS = 1800L;            // 30분
    public static final long LONG_TTL_SECONDS = 7200L;              // 2시간
    public static final long DAILY_TTL_SECONDS = 86400L;            // 24시간

    // TTL 설정 (분 단위)
    public static final long DEFAULT_TTL_MINUTES = 60L;
    public static final long SHORT_TTL_MINUTES = 5L;
    public static final long MEDIUM_TTL_MINUTES = 30L;
    public static final long LONG_TTL_MINUTES = 120L;

    // 캐시 키 프리픽스
    public static final String KEY_PREFIX_USER = "user:";
    public static final String KEY_PREFIX_PRODUCT = "product:";
    public static final String KEY_PREFIX_SESSION = "session:";
    public static final String KEY_PREFIX_TOKEN = "token:";
    public static final String KEY_PREFIX_TEMP = "temp:";

    // 캐시 키 구분자
    public static final String KEY_DELIMITER = ":";
    public static final String KEY_WILDCARD = "*";

    // Redis 설정
    public static final int REDIS_MAX_CONNECTIONS = 100;
    public static final int REDIS_MAX_CONNECTIONS_PER_ROUTE = 20;
    public static final int REDIS_CONNECTION_TIMEOUT_SECONDS = 5;
    public static final int REDIS_READ_TIMEOUT_SECONDS = 30;

    // 캐시 관련 메시지
    public static final String MSG_CACHE_HIT = "캐시 히트: key={}";
    public static final String MSG_CACHE_MISS = "캐시 미스: key={}";
    public static final String MSG_CACHE_SAVED = "캐시 저장: key={}, ttl={} {}";
    public static final String MSG_CACHE_DELETED = "캐시 삭제: key={}, deleted={}";
    public static final String MSG_CACHE_ERROR = "캐시 작업 실패: key={}";

}
