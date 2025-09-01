package com.pullit.application.auth.controller;

import com.pullit.application.auth.dto.VerificationRequest;
import com.pullit.application.auth.dto.VerificationResponse;
import com.pullit.application.auth.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/verification")
@RequiredArgsConstructor
@Tag(name = "Verification", description = "휴대폰 인증 API")
public class VerificationController {
    
    private final VerificationService verificationService;
    
    /**
     * 인증번호 발송
     */
    @PostMapping("/send")
    @Operation(summary = "인증번호 발송", description = "휴대폰 번호로 인증번호를 발송합니다.")
    public ResponseEntity<VerificationResponse> sendVerificationCode(
            @Valid @RequestBody VerificationRequest.Send request) {
        
        try {
            boolean result = verificationService.sendVerificationCode(request.getPhoneNumber());
            
            if (result) {
                return ResponseEntity.ok(
                    VerificationResponse.builder()
                        .success(true)
                        .message("인증번호가 발송되었습니다.")
                        .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                        .success(false)
                        .message("인증번호 발송에 실패했습니다.")
                        .build()
                );
            }
        } catch (Exception e) {
            log.error("Error sending verification code", e);
            return ResponseEntity.badRequest().body(
                VerificationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * 인증번호 검증
     */
    @PostMapping("/verify")
    @Operation(summary = "인증번호 검증", description = "발송된 인증번호를 검증합니다.")
    public ResponseEntity<VerificationResponse> verifyCode(
            @Valid @RequestBody VerificationRequest.Verify request) {
        
        try {
            boolean isValid = verificationService.verifyCode(
                request.getPhoneNumber(), 
                request.getCode()
            );
            
            if (isValid) {
                return ResponseEntity.ok(
                    VerificationResponse.builder()
                        .success(true)
                        .message("인증이 완료되었습니다.")
                        .verified(true)
                        .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                        .success(false)
                        .message("인증번호가 일치하지 않습니다.")
                        .verified(false)
                        .build()
                );
            }
        } catch (Exception e) {
            log.error("Error verifying code", e);
            return ResponseEntity.badRequest().body(
                VerificationResponse.builder()
                    .success(false)
                    .message("인증 처리 중 오류가 발생했습니다.")
                    .verified(false)
                    .build()
            );
        }
    }
    
    /**
     * 인증번호 재발송
     */
    @PostMapping("/resend")
    @Operation(summary = "인증번호 재발송", description = "인증번호를 재발송합니다.")
    public ResponseEntity<VerificationResponse> resendVerificationCode(
            @Valid @RequestBody VerificationRequest.Send request) {
        
        try {
            boolean result = verificationService.resendVerificationCode(request.getPhoneNumber());
            
            if (result) {
                return ResponseEntity.ok(
                    VerificationResponse.builder()
                        .success(true)
                        .message("인증번호가 재발송되었습니다.")
                        .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                    VerificationResponse.builder()
                        .success(false)
                        .message("인증번호 재발송에 실패했습니다.")
                        .build()
                );
            }
        } catch (Exception e) {
            log.error("Error resending verification code", e);
            return ResponseEntity.badRequest().body(
                VerificationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}