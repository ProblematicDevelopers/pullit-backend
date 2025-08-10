package com.pullit.common.annotation;

import java.lang.annotation.*;

/**
 * 실패 시 자동 재시도 어노테이션
 * 외부 API 호출이나 네트워크 작업에 유용
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retry {
    int maxAttempts() default 3;
    long delay() default 1000; // 밀리초
    double multiplier() default 1.5;
    Class<? extends Throwable>[] retryFor() default {Exception.class};
    Class<? extends Throwable>[] noRetryFor() default {};
}