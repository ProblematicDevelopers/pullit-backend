package com.pullit.common.constants;

public class ApiConstants {

    private ApiConstants() {}

    // ========== 인증 관련 엔드포인트 ==========
    public static final String AUTH_BASE = "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_REFRESH = "/refresh";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_VERIFY = "/verify";

    // ========== 사용자 관련 엔드포인트 ==========
    public static final String USER_BASE = "/users";
    public static final String USER_PROFILE = "/profile";
    public static final String USER_UPDATE = "/update";
    public static final String USER_DELETE = "/delete";
    public static final String USER_SEARCH = "/search";

    // ========== 공통 파라미터 이름 ==========
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_SORT = "sort";
    public static final String PARAM_SEARCH = "search";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_START_DATE = "startDate";
    public static final String PARAM_END_DATE = "endDate";

    // ========== 응답 메시지 ==========
    public static final String MSG_SUCCESS = "요청이 성공적으로 처리되었습니다.";
    public static final String MSG_CREATED = "리소스가 성공적으로 생성되었습니다.";
    public static final String MSG_UPDATED = "리소스가 성공적으로 수정되었습니다.";
    public static final String MSG_DELETED = "리소스가 성공적으로 삭제되었습니다.";
    public static final String MSG_NOT_FOUND = "요청한 리소스를 찾을 수 없습니다.";
    public static final String MSG_BAD_REQUEST = "잘못된 요청입니다.";
    public static final String MSG_UNAUTHORIZED = "인증이 필요합니다.";
    public static final String MSG_FORBIDDEN = "접근 권한이 없습니다.";

}
