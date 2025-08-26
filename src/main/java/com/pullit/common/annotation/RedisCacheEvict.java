package com.pullit.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCacheEvict {
    /**
     * 삭제할 캐시 키 (SpEL 표현식 지원)
     * allEntries가 false일 때 사용
     */
    String key() default "";

    /**
     * 삭제할 캐시 패턴 (와일드카드 지원)
     * allEntries가 true일 때 사용
     */
    String pattern() default "";

    /**
     * 모든 엔트리 삭제 여부
     * true이면 pattern을 사용하여 매칭되는 모든 키 삭제
     */
    boolean allEntries() default false;

    /**
     * 메소드 실행 전 캐시 삭제 여부
     * true: 메소드 실행 전 삭제 (트랜잭션과 무관)
     * false: 메소드 정상 실행 후 삭제 (기본값)
     */
    boolean beforeInvocation() default false;
}
