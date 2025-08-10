package com.pullit.common.aspect;

import com.pullit.common.annotation.LoggingTrace;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * @LoggingTrace 어노테이션 처리를 위한 Aspect
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {
    
    @Around("@annotation(loggingTrace)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint, LoggingTrace loggingTrace) throws Throwable {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 메소드 진입 로깅
            logEntry(loggingTrace, fullMethodName, joinPoint.getArgs());
            
            // 메소드 실행
            Object result = joinPoint.proceed();
            
            // 메소드 종료 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            logExit(loggingTrace, fullMethodName, result, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logError(loggingTrace, fullMethodName, e, executionTime);
            throw e;
        } finally {
            MDC.remove("traceId");
        }
    }
    
    private void logEntry(LoggingTrace trace, String methodName, Object[] args) {
        String message = String.format("→ 메소드 진입: %s", methodName);
        
        if (trace.logParameters() && args != null && args.length > 0) {
            message += String.format(" | 파라미터: %s", Arrays.toString(args));
        }
        
        logByLevel(trace.level(), message);
    }
    
    private void logExit(LoggingTrace trace, String methodName, Object result, long executionTime) {
        String message = String.format("← 메소드 종료: %s", methodName);
        
        if (trace.logReturnValue() && result != null) {
            message += String.format(" | 반환값: %s", result);
        }
        
        if (trace.logExecutionTime()) {
            message += String.format(" | 실행시간: %dms", executionTime);
        }
        
        logByLevel(trace.level(), message);
    }
    
    private void logError(LoggingTrace trace, String methodName, Exception e, long executionTime) {
        String message = String.format("✗ 메소드 실패: %s | 에러: %s | 실행시간: %dms", 
            methodName, e.getMessage(), executionTime);
        
        log.error(message, e);
    }
    
    private void logByLevel(LoggingTrace.LogLevel level, String message) {
        switch (level) {
            case TRACE -> log.trace(message);
            case DEBUG -> log.debug(message);
            case INFO -> log.info(message);
            case WARN -> log.warn(message);
            case ERROR -> log.error(message);
        }
    }
}