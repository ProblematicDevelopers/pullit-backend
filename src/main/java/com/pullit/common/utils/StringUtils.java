package com.pullit.common.utils;

import java.util.UUID;

public final class StringUtils {
    private StringUtils() {}

    // null/empty 체크
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    // 기본값 처리
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    public static String defaultIfNull(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }

    // 문자열 변환
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }

    public static String toUpperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }

    // 문자열 자르기
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    public static String truncateWithEllipsis(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    // 마스킹 처리
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local + "@" + domain;
        }

        return local.substring(0, 2) + "***@" + domain;
    }

    public static String maskPhone(String phone) {
        if (isEmpty(phone) || phone.length() < 8) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})(\\d{3,4})(\\d{4})", "$1-****-$3");
    }

    public static String maskName(String name) {
        if (isEmpty(name)) return name;

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        } else if (name.length() >= 3) {
            return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
        }
        return name;
    }

    // 랜덤 문자열 생성
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (chars.length() * Math.random())));
        }
        return sb.toString();
    }

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // 카멜케이스/스네이크케이스 변환
    public static String camelToSnake(String camel) {
        if (isEmpty(camel)) return camel;
        return camel.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    public static String snakeToCamel(String snake) {
        if (isEmpty(snake)) return snake;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (char c : snake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    // 패딩
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) return null;
        if (str.length() >= size) return str;

        StringBuilder sb = new StringBuilder(size);
        for (int i = str.length(); i < size; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    public static String rightPad(String str, int size, char padChar) {
        if (str == null) return null;
        if (str.length() >= size) return str;

        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < size) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    // HTML 이스케이프
    public static String escapeHtml(String html) {
        if (isEmpty(html)) return html;

        return html.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
