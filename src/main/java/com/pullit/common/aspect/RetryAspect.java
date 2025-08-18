package com.pullit.common.aspect;

import com.pullit.common.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @Retry 어노테이션 처리를 위한 Aspect
 * 실패 시 자동 재시도 로직 구현
 */
@Slf4j
@Aspect
@Component
public class RetryAspect {
    
    @Around("@annotation(retry)")
    public Object retryOnFailure(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        int maxAttempts = retry.maxAttempts();
        long delay = retry.delay();
        double multiplier = retry.multiplier();
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("메소드 실행 시도: {} - 시도 횟수: {}/{}", methodName, attempt, maxAttempts);
                
                // 메소드 실행
                Object result = joinPoint.proceed();
                
                if (attempt > 1) {
                    log.info("재시도 성공: {} - 시도 횟수: {}/{}", methodName, attempt, maxAttempts);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                // 재시도하지 않을 예외 확인
                if (shouldNotRetry(e, retry.noRetryFor())) {
                    log.error("재시도 제외 예외 발생: {} - {}", methodName, e.getClass().getSimpleName());
                    throw e;
                }
                
                // 재시도할 예외 확인
                if (!shouldRetry(e, retry.retryFor())) {
                    log.error("재시도 대상이 아닌 예외 발생: {} - {}", methodName, e.getClass().getSimpleName());
                    throw e;
                }
                
                if (attempt < maxAttempts) {
                    long currentDelay = (long) (delay * Math.pow(multiplier, attempt - 1));
                    log.warn("메소드 실행 실패: {} - 시도 {}/{} - {}ms 후 재시도 - 에러: {}",
                        methodName, attempt, maxAttempts, currentDelay, e.getMessage());
                    
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                } else {
                    log.error("모든 재시도 실패: {} - 최대 시도 횟수 {} 도달", methodName, maxAttempts);
                }
            }
        }
        
        throw lastException != null ? lastException : new RuntimeException("알 수 없는 오류 발생");
    }
    
    private boolean shouldRetry(Exception e, Class<? extends Throwable>[] retryFor) {
        return Arrays.stream(retryFor)
            .anyMatch(exClass -> exClass.isAssignableFrom(e.getClass()));
    }
    
    private boolean shouldNotRetry(Exception e, Class<? extends Throwable>[] noRetryFor) {
        return Arrays.stream(noRetryFor)
            .anyMatch(exClass -> exClass.isAssignableFrom(e.getClass()));
    }
}