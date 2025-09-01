package com.pullit.notification.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.pullit.notification.enums.NotificationType;

/**
 * 메소드 실행 후 알림을 자동으로 생성하는 커스텀 어노테이션
 * AOP를 통해 처리됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationTrigger {
    
    /**
     * 알림 타입
     */
    NotificationType type();
    
    /**
     * 알림을 받을 사용자 ID를 가져올 SpEL 표현식
     * 예: "#result.userId", "#student.id", "#request.userId"
     */
    String userIdExpression() default "";
    
    /**
     * 알림 제목 (선택사항, 비어있으면 타입의 기본 제목 사용)
     */
    String title() default "";
    
    /**
     * 알림 메시지 템플릿 (SpEL 지원)
     * 예: "'시험 ' + #result.examName + '이(가) 배정되었습니다'"
     */
    String message() default "";
    
    /**
     * 알림 클릭시 이동할 URL (SpEL 지원)
     * 예: "'/exam/' + #result.examId"
     */
    String targetUrl() default "";
    
    /**
     * 조건부 알림 발송 (SpEL 표현식)
     * true일 때만 알림 발송
     * 예: "#result.score > 80"
     */
    String condition() default "true";
    
    /**
     * 여러 사용자에게 알림을 보낼지 여부
     */
    boolean multipleUsers() default false;
    
    /**
     * 여러 사용자 ID를 가져올 SpEL 표현식 (multipleUsers가 true일 때 사용)
     * 예: "#result.studentIds", "#classRoom.students.![id]"
     */
    String userIdsExpression() default "";
}