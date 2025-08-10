package com.pullit.common.annotation;

import java.lang.annotation.*;

/**
 * 메소드 실행 시간 측정을 위한 어노테이션
 * AOP와 함께 사용하여 성능 모니터링
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimeExecution {
    String value() default "";
    boolean logArgs() default false;
    boolean logResult() default false;
    long warnThreshold() default 1000; // 밀리초 단위
}