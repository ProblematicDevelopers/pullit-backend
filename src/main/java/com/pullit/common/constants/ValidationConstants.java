package com.pullit.common.constants;

public class ValidationConstants {
    private ValidationConstants() {}

    // ========== 길이 제한 ==========
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 100;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_CONTENT_LENGTH = 5000;

    // ========== 검증 메시지 ==========
    public static final String MSG_REQUIRED = "필수 입력 항목입니다.";
    public static final String MSG_INVALID_EMAIL = "올바른 이메일 형식이 아닙니다.";
    public static final String MSG_INVALID_PHONE = "올바른 전화번호 형식이 아닙니다.";
    public static final String MSG_INVALID_PASSWORD = "비밀번호는 최소 8자 이상, 대소문자, 숫자, 특수문자를 포함해야 합니다.";
    public static final String MSG_PASSWORD_MISMATCH = "비밀번호가 일치하지 않습니다.";
    public static final String MSG_DUPLICATE_EMAIL = "이미 사용 중인 이메일입니다.";
    public static final String MSG_DUPLICATE_USERNAME = "이미 사용 중인 사용자명입니다.";
    public static final String MSG_INVALID_DATE_FORMAT = "올바른 날짜 형식이 아닙니다.";
    public static final String MSG_INVALID_DATE_RANGE = "시작일은 종료일보다 이전이어야 합니다.";

    // ========== 숫자 범위 ==========
    public static final int MIN_AGE = 1;
    public static final int MAX_AGE = 150;
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 9999;

}
