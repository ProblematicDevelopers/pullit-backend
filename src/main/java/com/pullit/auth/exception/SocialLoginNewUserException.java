package com.pullit.auth.exception;

import java.util.Map;

/**
 * 소셜 로그인 시 신규 사용자인 경우 발생하는 예외
 */
public class SocialLoginNewUserException extends RuntimeException {
    
    private final Map<String, Object> socialInfo;
    
    public SocialLoginNewUserException(String message) {
        super(message);
        this.socialInfo = null;
    }
    
    public SocialLoginNewUserException(String message, Throwable cause) {
        super(message, cause);
        this.socialInfo = null;
    }
    
    public SocialLoginNewUserException(String message, Map<String, Object> socialInfo) {
        super(message);
        this.socialInfo = socialInfo;
    }
    
    public Map<String, Object> getSocialInfo() {
        return socialInfo;
    }
} 