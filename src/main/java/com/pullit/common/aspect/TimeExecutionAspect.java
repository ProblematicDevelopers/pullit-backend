package com.pullit.common.aspect;

import com.pullit.common.annotation.TimeExecution;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @TimeExecution 어노테이션 처리를 위한 Aspect
 */
@Slf4j
@Aspect
@Component
public class TimeExecutionAspect {
    
    @Around("@annotation(timeExecution)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, TimeExecution timeExecution) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        // 파라미터 로깅
        if (timeExecution.logArgs()) {
            log.debug("메소드 호출: {} - 파라미터: {}", methodName, Arrays.toString(joinPoint.getArgs()));
        }
        
        try {
            // 메소드 실행
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 결과 로깅
            if (timeExecution.logResult()) {
                log.debug("메소드 결과: {} - 반환값: {}", methodName, result);
            }
            
            // 경고 임계값 초과 시 WARN 로그
            if (executionTime > timeExecution.warnThreshold()) {
                log.warn("메소드 실행 시간 초과: {} - {}ms (임계값: {}ms)",
                    methodName, executionTime, timeExecution.warnThreshold());
            } else {
                log.info(" 메소드 실행 완료: {} - {}ms", methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("메소드 실행 실패: {} - {}ms - 에러: {}",
                methodName, executionTime, e.getMessage(), e);
            throw e;
        }
    }
}