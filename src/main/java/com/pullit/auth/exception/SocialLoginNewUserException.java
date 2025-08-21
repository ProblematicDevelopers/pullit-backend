package com.pullit.auth.exception;

/**
 * 소셜 로그인 시 신규 사용자인 경우 발생하는 예외
 */
public class SocialLoginNewUserException extends RuntimeException {
    
    public SocialLoginNewUserException(String message) {
        super(message);
    }
    
    public SocialLoginNewUserException(String message, Throwable cause) {
        super(message, cause);
    }
} 