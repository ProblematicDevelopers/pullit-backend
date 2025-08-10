package com.pullit.common.utils;

import com.pullit.common.constants.AppConstants;
import com.pullit.common.constants.ValidationConstants;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private ValidationUtils(){}

    // 컴파일된 패턴 (성능 향상을 위해 재사용)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(ValidationConstants.PATTERN_EMAIL);
    private static final Pattern PHONE_PATTERN = Pattern.compile(ValidationConstants.PATTERN_PHONE);
    private static final Pattern PHONE_CLEAN_PATTERN = Pattern.compile(ValidationConstants.PATTERN_PHONE_CLEAN);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(ValidationConstants.PATTERN_PASSWORD);
    private static final Pattern USERNAME_PATTERN = Pattern.compile(ValidationConstants.PATTERN_USERNAME);
    private static final Pattern KOREAN_NAME_PATTERN = Pattern.compile(ValidationConstants.PATTERN_KOREAN_NAME);
    private static final Pattern ENGLISH_NAME_PATTERN = Pattern.compile(ValidationConstants.PATTERN_ENGLISH_NAME);
    private static final Pattern BUSINESS_NUMBER_PATTERN = Pattern.compile(ValidationConstants.PATTERN_BUSINESS_NUMBER);
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(ValidationConstants.PATTERN_IP_ADDRESS);
    private static final Pattern URL_PATTERN = Pattern.compile(ValidationConstants.PATTERN_URL);
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(ValidationConstants.PATTERN_NUMERIC);
    private static final Pattern ALPHA_PATTERN = Pattern.compile(ValidationConstants.PATTERN_ALPHA);
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(ValidationConstants.PATTERN_ALPHANUMERIC);

    // 이메일 검증
    public static boolean isValidEmail(String email) {
        if (StringUtils.isEmpty(email)) return false;
        if (email.length() > ValidationConstants.MAX_EMAIL_LENGTH) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    // 전화번호 검증
    public static boolean isValidPhone(String phone) {
        if (StringUtils.isEmpty(phone)) return false;
        String cleanPhone = phone.replaceAll("-", "");
        return PHONE_CLEAN_PATTERN.matcher(cleanPhone).matches();
    }

    // 비밀번호 검증
    public static boolean isValidPassword(String password) {
        if (StringUtils.isEmpty(password)) return false;
        if (password.length() < ValidationConstants.MIN_PASSWORD_LENGTH ||
                password.length() > ValidationConstants.MAX_PASSWORD_LENGTH) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // 사용자명 검증
    public static boolean isValidUsername(String username) {
        if (StringUtils.isEmpty(username)) return false;
        if (username.length() < ValidationConstants.MIN_USERNAME_LENGTH ||
                username.length() > ValidationConstants.MAX_USERNAME_LENGTH) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    // 한글 이름 검증
    public static boolean isValidKoreanName(String name) {
        if (StringUtils.isEmpty(name)) return false;
        if (name.length() > ValidationConstants.MAX_NAME_LENGTH) return false;
        return KOREAN_NAME_PATTERN.matcher(name).matches();
    }

    // 영문 이름 검증
    public static boolean isValidEnglishName(String name) {
        if (StringUtils.isEmpty(name)) return false;
        if (name.length() > ValidationConstants.MAX_NAME_LENGTH) return false;
        return ENGLISH_NAME_PATTERN.matcher(name).matches();
    }

    // 사업자번호 검증
    public static boolean isValidBusinessNumber(String businessNumber) {
        if (StringUtils.isEmpty(businessNumber)) return false;

        // 형식 검증
        if (!BUSINESS_NUMBER_PATTERN.matcher(businessNumber).matches()) {
            return false;
        }

        // 체크섬 검증
        String number = businessNumber.replaceAll("-", "");
        int[] keys = {1, 3, 7, 1, 3, 7, 1, 3, 5};
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(number.charAt(i)) * keys[i];
        }

        sum += (Character.getNumericValue(number.charAt(8)) * 5) / 10;
        int checkNum = (10 - (sum % 10)) % 10;

        return checkNum == Character.getNumericValue(number.charAt(9));
    }

    // URL 검증
    public static boolean isValidUrl(String url) {
        if (StringUtils.isEmpty(url)) return false;
        return URL_PATTERN.matcher(url).matches();
    }

    // IP 주소 검증
    public static boolean isValidIpAddress(String ip) {
        if (StringUtils.isEmpty(ip)) return false;
        return IP_ADDRESS_PATTERN.matcher(ip).matches();
    }

    // 숫자만 포함 검증
    public static boolean isNumeric(String str) {
        if (StringUtils.isEmpty(str)) return false;
        return NUMERIC_PATTERN.matcher(str).matches();
    }

    // 알파벳만 포함 검증
    public static boolean isAlpha(String str) {
        if (StringUtils.isEmpty(str)) return false;
        return ALPHA_PATTERN.matcher(str).matches();
    }

    // 알파벳과 숫자만 포함 검증
    public static boolean isAlphanumeric(String str) {
        if (StringUtils.isEmpty(str)) return false;
        return ALPHANUMERIC_PATTERN.matcher(str).matches();
    }

    // 신용카드 번호 검증 (Luhn 알고리즘)
    public static boolean isValidCreditCard(String cardNumber) {
        if (StringUtils.isEmpty(cardNumber)) return false;

        String cleaned = cardNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < ValidationConstants.MIN_CREDIT_CARD_LENGTH ||
                cleaned.length() > ValidationConstants.MAX_CREDIT_CARD_LENGTH) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;

        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cleaned.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10 == 0);
    }

    // 파일 확장자 검증 (AppConstants의 ALLOWED_EXTENSIONS 활용)
    public static boolean isValidImageFile(String filename) {
        if (StringUtils.isEmpty(filename)) return false;

        String extension = getFileExtension(filename);
        if (extension == null) return false;

        for (String allowed : AppConstants.ALLOWED_IMAGE_EXTENSIONS) {
            if (extension.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidDocumentFile(String filename) {
        if (StringUtils.isEmpty(filename)) return false;

        String extension = getFileExtension(filename);
        if (extension == null) return false;

        for (String allowed : AppConstants.ALLOWED_DOCUMENT_EXTENSIONS) {
            if (extension.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) return null;
        return filename.substring(lastDotIndex + 1);
    }

    // 나이 범위 검증
    public static boolean isValidAge(int age) {
        return age >= ValidationConstants.MIN_AGE && age <= ValidationConstants.MAX_AGE;
    }



    // 수량 범위 검증
    public static boolean isValidQuantity(int quantity) {
        return quantity >= ValidationConstants.MIN_QUANTITY && quantity <= ValidationConstants.MAX_QUANTITY;
    }

}
