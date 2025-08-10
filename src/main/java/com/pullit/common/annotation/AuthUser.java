package com.pullit.common.annotation;

import java.lang.annotation.*;

/**
 * 컨트롤러 메소드 파라미터에 현재 인증된 사용자 정보 주입
 * HandlerMethodArgumentResolver와 함께 사용
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthUser {
    boolean required() default true;
}