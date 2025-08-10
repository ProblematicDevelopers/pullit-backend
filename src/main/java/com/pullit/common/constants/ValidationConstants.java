package com.pullit.common.constants;

public class ValidationConstants {
    private ValidationConstants() {}

    // ========== 정규표현식 패턴 ==========
    public static final String PATTERN_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PATTERN_PHONE = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";
    public static final String PATTERN_PHONE_CLEAN = "^(01[016789])(\\d{3,4})(\\d{4})$";
    public static final String PATTERN_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    public static final String PATTERN_USERNAME = "^[a-zA-Z0-9_]{3,20}$";
    public static final String PATTERN_KOREAN_NAME = "^[가-힣]{2,10}$";
    public static final String PATTERN_ENGLISH_NAME = "^[a-zA-Z\\s]{2,50}$";
    public static final String PATTERN_BUSINESS_NUMBER = "^\\d{3}-\\d{2}-\\d{5}$";
    public static final String PATTERN_RESIDENT_NUMBER = "^\\d{6}-?[1-4]\\d{6}$";
    public static final String PATTERN_ZIP_CODE = "^\\d{5}$";
    public static final String PATTERN_IP_ADDRESS = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    public static final String PATTERN_URL = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
    public static final String PATTERN_ALPHANUMERIC = "^[a-zA-Z0-9]+$";
    public static final String PATTERN_NUMERIC = "^[0-9]+$";
    public static final String PATTERN_ALPHA = "^[a-zA-Z]+$";

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
    public static final int MIN_CREDIT_CARD_LENGTH = 13;
    public static final int MAX_CREDIT_CARD_LENGTH = 19;

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
    public static final String MSG_INVALID_BUSINESS_NUMBER = "올바른 사업자번호 형식이 아닙니다.";
    public static final String MSG_INVALID_CREDIT_CARD = "올바른 신용카드 번호가 아닙니다.";
    public static final String MSG_INVALID_URL = "올바른 URL 형식이 아닙니다.";
    public static final String MSG_INVALID_IP = "올바른 IP 주소 형식이 아닙니다.";

    // ========== 숫자 범위 ==========
    public static final int MIN_AGE = 1;
    public static final int MAX_AGE = 150;
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 9999;



}
