package com.pullit.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * API 요청 횟수 제한을 위한 어노테이션
 * Redis와 함께 사용하여 Rate Limiting 구현
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {
    int limit() default 100;
    int duration() default 1;
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    String key() default "";
}