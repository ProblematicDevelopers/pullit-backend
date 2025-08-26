package com.pullit.common.aspect;

import com.pullit.common.annotation.RedisCacheEvict;
import com.pullit.common.cache.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Redis 캐시 무효화를 위한 AOP Aspect
 * @RedisCacheEvict 어노테이션이 붙은 메소드 실행 시 캐시를 삭제
 * 
 * 주로 데이터 변경 작업(CREATE, UPDATE, DELETE) 후 캐시 무효화에 사용
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class RedisCacheEvictAspect {

    private final RedisCacheService cacheService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 메소드 실행 전 캐시 삭제 (beforeInvocation = true인 경우)
     * 
     * 메소드 실행 실패와 관계없이 캐시를 먼저 삭제해야 하는 경우 사용
     * 예: 트랜잭션이 없는 작업이나 캐시 정합성이 중요한 경우
     */
    @Before("@annotation(cacheEvict) && args(..)")
    public void evictCacheBefore(JoinPoint joinPoint, RedisCacheEvict cacheEvict) {
        if (cacheEvict.beforeInvocation()) {
            performCacheEviction(joinPoint, cacheEvict);
        }
    }

    /**
     * 메소드 정상 실행 후 캐시 삭제 (기본 동작)
     * 
     * 메소드가 성공적으로 완료된 후에만 캐시를 삭제
     * 예: 트랜잭션이 커밋된 후 캐시 무효화
     */
    @AfterReturning("@annotation(cacheEvict)")
    public void evictCacheAfter(JoinPoint joinPoint, RedisCacheEvict cacheEvict) {
        if (!cacheEvict.beforeInvocation()) {
            performCacheEviction(joinPoint, cacheEvict);
        }
    }

    /**
     * 실제 캐시 삭제 로직 수행
     */
    private void performCacheEviction(JoinPoint joinPoint, RedisCacheEvict cacheEvict) {
        try {
            if (cacheEvict.allEntries()) {
                // 모든 캐시 엔트리 삭제 (패턴 기반)
                String pattern = generateCachePattern(cacheEvict.pattern(), joinPoint);
                cacheService.evictByPattern(pattern);
                log.info("패턴 기반 캐시 삭제: pattern={}", pattern);
            } else {
                // 특정 키만 삭제
                String cacheKey = generateCacheKey(cacheEvict.key(), joinPoint);
                cacheService.evict(cacheKey);
                log.info("캐시 삭제: key={}", cacheKey);
            }
        } catch (Exception e) {
            log.error("캐시 삭제 실패", e);
            // 캐시 삭제 실패가 비즈니스 로직에 영향을 주지 않도록 예외를 전파하지 않음
        }
    }

    /**
     * 캐시 패턴 생성 (와일드카드 사용)
     * 
     * 예시:
     * - "user:*" : user로 시작하는 모든 키
     * - "*:findById:*" : findById 메소드의 모든 캐시
     */
    private String generateCachePattern(String pattern, JoinPoint joinPoint) {
        if (!StringUtils.hasText(pattern)) {
            // 기본 패턴: 클래스명:*
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            return signature.getDeclaringType().getSimpleName() + ":*";
        }
        
        // SpEL 표현식 처리
        if (pattern.contains("#")) {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expression = parser.parseExpression(pattern);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : pattern;
        }
        
        return pattern;
    }

    /**
     * 캐시 키 생성 (RedisCacheAspect와 동일한 로직)
     */
    private String generateCacheKey(String keyExpression, JoinPoint joinPoint) {
        // RedisCacheAspect의 generateCacheKey와 동일한 구현
        // 코드 중복을 피하기 위해 공통 유틸리티 클래스로 추출 가능
        if (!StringUtils.hasText(keyExpression)) {
            return generateDefaultCacheKey(joinPoint);
        }

        if (keyExpression.contains("#")) {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expression = parser.parseExpression(keyExpression);
            Object keyValue = expression.getValue(context);
            return keyValue != null ? keyValue.toString() : generateDefaultCacheKey(joinPoint);
        }
        
        return keyExpression;
    }

    private String generateDefaultCacheKey(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + ":" + 
               signature.getMethod().getName();
    }

    private EvaluationContext createEvaluationContext(JoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        
        return context;
    }
}