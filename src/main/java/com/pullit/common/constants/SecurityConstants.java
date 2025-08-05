package com.pullit.common.constants;

public final class SecurityConstants {
    
    private SecurityConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // API 경로
    public static final String API_PREFIX = "/api";
    public static final String AUTH_PREFIX = API_PREFIX + "/auth";
    
    // 공개 접근 가능 경로
    public static final String[] PUBLIC_PATHS = {
        "/api/health",
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/questions/public/**",  // 공개 문제 조회
        "/api/exams/public/**",      // 공개 시험 조회
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**"
    };
    
    // 문제은행 API 경로
    public static final String QUESTIONS_API = API_PREFIX + "/questions";
    public static final String EXAMS_API = API_PREFIX + "/exams";
    public static final String SUBMISSIONS_API = API_PREFIX + "/submissions";
    public static final String CATEGORIES_API = API_PREFIX + "/categories";
    
    // 인증 헤더
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // JWT 클레임
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    
    // 권한
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_EXAM_CREATOR = "ROLE_EXAM_CREATOR";
    public static final String ROLE_QUESTION_REVIEWER = "ROLE_QUESTION_REVIEWER";
    
    // 에러 메시지
    public static final String INVALID_TOKEN_MESSAGE = "Invalid authentication token";
    public static final String EXPIRED_TOKEN_MESSAGE = "Authentication token has expired";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized access";
    public static final String FORBIDDEN_MESSAGE = "Access forbidden";
}