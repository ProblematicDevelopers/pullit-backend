package com.pullit.common.constants;

public class HttpConstants {

    private HttpConstants() {}

    // 타임아웃 설정 (밀리초)
    public static final int CONNECT_TIMEOUT_MS = 5000;           // 연결 타임아웃 5초
    public static final int READ_TIMEOUT_MS = 30000;             // 읽기 타임아웃 30초
    public static final int WRITE_TIMEOUT_MS = 10000;            // 쓰기 타임아웃 10초
    public static final int CONNECTION_REQUEST_TIMEOUT_MS = 3000; // 풀에서 연결 획득 타임아웃 3초

    // 연결 풀 설정
    public static final int MAX_TOTAL_CONNECTIONS = 100;          // 전체 최대 연결 수
    public static final int MAX_CONNECTIONS_PER_ROUTE = 20;       // 호스트당 최대 연결 수
    public static final int IDLE_CONNECTION_TIMEOUT_SECONDS = 30; // 유휴 연결 타임아웃
    public static final int CONNECTION_MAX_LIFETIME_MINUTES = 5;  // 연결 최대 생존 시간

    // 재시도 설정
    public static final int MAX_RETRY_ATTEMPTS = 3;              // 최대 재시도 횟수
    public static final long RETRY_DELAY_MS = 1000L;             // 재시도 지연 시간 (밀리초)
    public static final double RETRY_BACKOFF_MULTIPLIER = 2.0;   // 지수 백오프 배수

    // 버퍼 크기
    public static final int BUFFER_SIZE = 8192;                  // 8KB
    public static final int MAX_IN_MEMORY_SIZE = 10485760;       // 10MB

    // HTTP 헤더
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_X_REQUEST_ID = "X-Request-ID";
    public static final String HEADER_X_API_KEY = "X-API-Key";

    // 미디어 타입
    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String MEDIA_TYPE_XML = "application/xml";
    public static final String MEDIA_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MEDIA_TYPE_MULTIPART = "multipart/form-data";

    // 기본 User-Agent
    public static final String DEFAULT_USER_AGENT = "Pullit-HTTP-Client/1.0";

    // 로그 메시지
    public static final String LOG_REQUEST = "HTTP 요청: {} {}";
    public static final String LOG_RESPONSE = "HTTP 응답: {} ({}ms)";
    public static final String LOG_RETRY = "HTTP 요청 실패 (시도 {}/{}): {}";
    public static final String LOG_ERROR = "HTTP 에러: {} - {}";

}
