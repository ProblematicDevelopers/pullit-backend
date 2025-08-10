package com.pullit.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐싱을 위한 커스텀 어노테이션
 * Spring Cache보다 세밀한 제어 가능
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCacheable {
    String key() default "";
    long ttl() default 60;
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    boolean compress() default false;
    String condition() default "";
}