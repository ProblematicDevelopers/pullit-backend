package com.pullit.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VerificationRequest {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Send {
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", 
                message = "올바른 전화번호 형식이 아닙니다.")
        private String phoneNumber;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Verify {
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", 
                message = "올바른 전화번호 형식이 아닙니다.")
        private String phoneNumber;
        
        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자입니다.")
        private String code;
    }
}