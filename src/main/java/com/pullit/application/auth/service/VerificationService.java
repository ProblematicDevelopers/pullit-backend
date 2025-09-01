package com.pullit.application.auth.service;

import com.pullit.infrastructure.sms.config.SmsConfig;
import com.pullit.infrastructure.sms.service.CoolSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {
    
    private final CoolSmsService smsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SmsConfig smsConfig;
    
    private static final String VERIFICATION_PREFIX = "verification:phone:";
    private static final String ATTEMPT_PREFIX = "attempt:phone:";
    private static final int MAX_ATTEMPTS_PER_DAY = 5;
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * 인증번호 발송
     * @param phoneNumber 전화번호
     * @return 발송 성공 여부
     */
    public boolean sendVerificationCode(String phoneNumber) {
        // 일일 발송 제한 체크
        if (!checkDailyLimit(phoneNumber)) {
            log.warn("Daily SMS limit exceeded for phone: {}", phoneNumber);
            throw new RuntimeException("일일 SMS 발송 한도를 초과했습니다.");
        }
        
        // 인증번호 생성
        String verificationCode = generateVerificationCode();
        
        // Redis에 저장 (TTL 설정)
        String key = VERIFICATION_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(
            key, 
            verificationCode, 
            smsConfig.getVerificationCodeTtl(), 
            TimeUnit.SECONDS
        );
        
        // 발송 시도 횟수 증가
        incrementAttemptCount(phoneNumber);
        
        // SMS 발송
        boolean result = smsService.sendVerificationCode(phoneNumber, verificationCode);
        
        if (result) {
            log.info("Verification code sent to: {}", phoneNumber);
        } else {
            // 발송 실패 시 Redis에서 삭제
            redisTemplate.delete(key);
            log.error("Failed to send verification code to: {}", phoneNumber);
        }
        
        return result;
    }
    
    /**
     * 인증번호 검증
     * @param phoneNumber 전화번호
     * @param code 인증번호
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String phoneNumber, String code) {
        String key = VERIFICATION_PREFIX + phoneNumber;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("No verification code found for phone: {}", phoneNumber);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        
        if (isValid) {
            // 검증 성공 시 Redis에서 삭제
            redisTemplate.delete(key);
            log.info("Verification successful for phone: {}", phoneNumber);
        } else {
            log.warn("Invalid verification code for phone: {}", phoneNumber);
        }
        
        return isValid;
    }
    
    /**
     * 인증번호 재발송
     * @param phoneNumber 전화번호
     * @return 발송 성공 여부
     */
    public boolean resendVerificationCode(String phoneNumber) {
        // 기존 인증번호 삭제
        String key = VERIFICATION_PREFIX + phoneNumber;
        redisTemplate.delete(key);
        
        // 새로운 인증번호 발송
        return sendVerificationCode(phoneNumber);
    }
    
    /**
     * 인증번호 생성 (6자리 숫자)
     */
    private String generateVerificationCode() {
        int codeLength = smsConfig.getVerificationCodeLength();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }
    
    /**
     * 일일 발송 제한 체크
     */
    private boolean checkDailyLimit(String phoneNumber) {
        String key = ATTEMPT_PREFIX + phoneNumber;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts == null) {
            return true;
        }
        
        return Integer.parseInt(attempts) < MAX_ATTEMPTS_PER_DAY;
    }
    
    /**
     * 발송 시도 횟수 증가
     */
    private void incrementAttemptCount(String phoneNumber) {
        String key = ATTEMPT_PREFIX + phoneNumber;
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        if (attempts != null && attempts == 1) {
            // 첫 시도인 경우 자정까지 TTL 설정
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }
}