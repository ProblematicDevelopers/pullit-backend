package com.pullit.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring AOP 설정 클래스
 * 
 * @EnableAspectJAutoProxy: AspectJ 스타일의 AOP를 활성화
 * - proxyTargetClass = true: CGLIB 프록시 사용 (인터페이스가 없는 클래스도 프록시 가능)
 * - exposeProxy = true: AopContext를 통해 현재 프록시에 접근 가능
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class AopConfig {
    
    /**
     * AOP 관련 추가 설정이 필요한 경우 여기에 Bean 정의
     * 
     * 예시:
     * - 커스텀 Advisor
     * - PointcutAdvisor
     * - 공통 Pointcut 정의
     */
}