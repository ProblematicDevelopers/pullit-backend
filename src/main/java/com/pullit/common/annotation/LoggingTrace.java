package com.pullit.common.annotation;

import java.lang.annotation.*;

/**
 * 메소드 진입/종료 로깅 및 추적을 위한 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoggingTrace {
    LogLevel level() default LogLevel.INFO;
    boolean logParameters() default true;
    boolean logReturnValue() default true;
    boolean logExecutionTime() default true;
    
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}