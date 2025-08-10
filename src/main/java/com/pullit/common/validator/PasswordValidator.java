package com.pullit.common.validator;

import com.pullit.common.annotation.ValidPassword;
import com.pullit.common.utils.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @ValidPassword 어노테이션의 검증 로직 구현
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    
    @Override
    public void initialize(ValidPassword annotation) {
        this.minLength = annotation.minLength();
        this.requireUppercase = annotation.requireUppercase();
        this.requireLowercase = annotation.requireLowercase();
        this.requireDigit = annotation.requireDigit();
        this.requireSpecialChar = annotation.requireSpecialChar();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        
        // 최소 길이 검증
        if (password.length() < minLength) {
            return false;
        }
        
        // 대문자 포함 검증
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // 소문자 포함 검증
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            return false;
        }
        
        // 숫자 포함 검증
        if (requireDigit && !password.matches(".*\\d.*")) {
            return false;
        }
        
        // 특수문자 포함 검증
        if (requireSpecialChar && !password.matches(".*[@$!%*?&#].*")) {
            return false;
        }
        
        return true;
    }
}