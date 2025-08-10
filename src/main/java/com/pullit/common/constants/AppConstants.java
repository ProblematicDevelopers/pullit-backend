package com.pullit.common.constants;

public class AppConstants {

    private AppConstants() {}
    // ========== 페이징 관련 상수 ==========
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // ========== 시간 관련 상수 ==========
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String TIMEZONE = "Asia/Seoul";
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int CACHE_EXPIRY_MINUTES = 10;

    // ========== 파일 업로드 관련 상수 ==========
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String UPLOAD_DIR = "uploads";
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    public static final String[] ALLOWED_DOCUMENT_EXTENSIONS = {"pdf", "doc", "docx", "xls", "xlsx"};

    // ========== HTTP 헤더 관련 상수 ==========
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER = "Bearer ";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";

    // ========== 정규표현식 패턴 ==========
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE_PATTERN = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";

    // ========== 캐시 키 접두사 ==========
    public static final String CACHE_KEY_USER = "user:";
    public static final String CACHE_KEY_TOKEN = "token:";
    public static final String CACHE_KEY_SESSION = "session:";
    public static final String CACHE_KEY_TEMP = "temp:";

    // ========== 상태 코드 ==========
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_BLOCKED = "BLOCKED";

}
