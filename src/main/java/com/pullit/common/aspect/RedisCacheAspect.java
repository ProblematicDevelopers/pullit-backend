package com.pullit.common.aspect;

import com.pullit.common.annotation.RedisCacheable;
import com.pullit.common.cache.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class RedisCacheAspect {

    private final RedisCacheService redisCacheService;
    //Spring Expression Parser 동적키 생성에 사용
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(cacheable)")
    public Object cacheableMethod(ProceedingJoinPoint joinPoint, RedisCacheable cacheable) throws Throwable {
        // 1. 캐싱 조건 확인 (condition 속성이 있는 경우)
        if (!evaluateCondition(cacheable.condition(), joinPoint)) {
            log.debug("캐시 조건 미충족, 메소드 직접 실행: {}", 
                joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }

        // 2. 캐시 키 생성
        String cacheKey = generateCacheKey(cacheable.key(), joinPoint);
        log.debug("생성된 캐시 키: {}", cacheKey);

        // 3. 메소드 반환 타입 정보 획득
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        // 4. 캐시에서 값 조회 시도
        Object cachedValue = redisCacheService.get(cacheKey, returnType);
        
        if (cachedValue != null) {
            // 캐시 히트 - 캐시된 값 반환
            log.info("캐시 히트 - 메소드: {}, 키: {}", 
                signature.getMethod().getName(), cacheKey);
            return cachedValue;
        }

        // 5. 캐시 미스 - 실제 메소드 실행
        log.info("캐시 미스 - 메소드 실행: {}", signature.getMethod().getName());
        Object result = joinPoint.proceed();

        // 6. 실행 결과를 캐시에 저장 (null이 아닌 경우만)
        if (result != null) {
            redisCacheService.put(cacheKey, result, cacheable.ttl(), cacheable.timeUnit());
            log.info("캐시 저장 - 키: {}, TTL: {} {}", 
                cacheKey, cacheable.ttl(), cacheable.timeUnit());
        }

        return result;
    }

    /**
     * SpEL 표현식을 평가하여 캐싱 조건을 확인
     *
     * @param condition SpEL 표현식 문자열
     * @param joinPoint 메소드 정보
     * @return 조건 만족 여부 (true: 캐싱 진행, false: 캐싱 스킵)
     */
    private boolean evaluateCondition(String condition, ProceedingJoinPoint joinPoint) {
        if(!StringUtils.hasText(condition)){
            return true;
        }
        try{
            EvaluationContext context = createEvaluationContext(joinPoint);

            Expression expression = parser.parseExpression(condition);
            Boolean result = expression.getValue(context, Boolean.class);

            return result !=null ? result : true;
        }catch(Exception e ){
            log.warn("캐시 평가 실패 , 캐시진행 {}", e.getMessage());
            return true;
        }
    }

    /**
     * 캐시 키 생성 로직
     *
     * SpEL 표현식을 지원하여 동적으로 키를 생성할 수 있음
     * 예시:
     * - "#userId" : userId 파라미터 사용
     * - "#user.id" : user 객체의 id 속성 사용
     * - "'user:' + #userId" : 문자열 조합
     *
     * @param keyExpression SpEL 표현식 또는 고정 키
     * @param joinPoint 메소드 정보
     * @return 생성된 캐시 키
     */
    private String generateCacheKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        if(!StringUtils.hasText(keyExpression)){
            return generateDefaultCacheKey(joinPoint);
        }
        try {
            // SpEL 표현식인지 확인 (#으로 시작하면 SpEL)
            if (keyExpression.contains("#")) {
                // SpEL 평가 컨텍스트 생성
                EvaluationContext context = createEvaluationContext(joinPoint);

                // 표현식 파싱 및 평가
                Expression expression = parser.parseExpression(keyExpression);
                Object keyValue = expression.getValue(context);

                return keyValue != null ? keyValue.toString() : generateDefaultCacheKey(joinPoint);
            } else {
                // 고정 키인 경우 그대로 사용
                return keyExpression;
            }
        } catch (Exception e) {
            log.warn("캐시 키 생성 실패, 기본 키 사용: {}", e.getMessage());
            return generateDefaultCacheKey(joinPoint);
        }

    }

    private String generateDefaultCacheKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StringBuilder keyBuilder = new StringBuilder();

        keyBuilder.append(signature.getDeclaringType().getSimpleName());
        keyBuilder.append(":");

        keyBuilder.append(signature.getMethod().getName());

        Object[] args = joinPoint.getArgs();
        if(args != null && args.length > 0){
            keyBuilder.append(":");
            for(Object arg : args){
                if(arg!=null){
                    keyBuilder.append(arg.hashCode()).append("_");
                }
            }
        }
        return keyBuilder.toString();
    }

    /**
     * SpEL 평가를 위한 컨텍스트 생성
     *
     * 메소드 파라미터들을 변수로 등록하여 SpEL 표현식에서 사용 가능하게 함
     *
     * @param joinPoint 메소드 정보
     * @return SpEL 평가 컨텍스트
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 메소드 시그니처 정보
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        // 파라미터를 컨텍스트에 변수로 등록
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 메타 정보 추가 (SpEL에서 사용 가능)
        context.setVariable("method", method);
        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("args", args);

        return context;
    }
}
