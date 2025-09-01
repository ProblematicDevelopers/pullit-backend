package com.pullit.notification.aspect;

import com.pullit.notification.annotation.NotificationTrigger;
import com.pullit.notification.dto.request.NotificationCreateRequest;
import com.pullit.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificationAspect {
    
    private final NotificationService notificationService;
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @AfterReturning(
        pointcut = "@annotation(notificationTrigger)",
        returning = "result"
    )
    public void handleNotificationTrigger(
            JoinPoint joinPoint,
            NotificationTrigger notificationTrigger,
            Object result) {
        
        try {
            // SpEL 평가 컨텍스트 생성
            EvaluationContext context = createEvaluationContext(joinPoint, result);
            
            // 조건 확인
            if (!evaluateCondition(notificationTrigger.condition(), context)) {
                log.debug("Notification condition not met, skipping notification");
                return;
            }
            
            // 알림 생성
            if (notificationTrigger.multipleUsers()) {
                handleMultipleUsers(notificationTrigger, context);
            } else {
                handleSingleUser(notificationTrigger, context);
            }
            
        } catch (Exception e) {
            log.error("Failed to send notification: ", e);
        }
    }
    
    private void handleSingleUser(NotificationTrigger trigger, EvaluationContext context) {
        Long userId = evaluateUserId(trigger.userIdExpression(), context);
        if (userId == null) {
            log.warn("Could not determine user ID for notification");
            return;
        }
        
        NotificationCreateRequest request = buildNotificationRequest(trigger, context, userId);
        notificationService.createNotification(request);
    }
    
    private void handleMultipleUsers(NotificationTrigger trigger, EvaluationContext context) {
        List<Long> userIds = evaluateUserIds(trigger.userIdsExpression(), context);
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs found for multiple user notification");
            return;
        }
        
        for (Long userId : userIds) {
            NotificationCreateRequest request = buildNotificationRequest(trigger, context, userId);
            notificationService.createNotification(request);
        }
    }
    
    private NotificationCreateRequest buildNotificationRequest(
            NotificationTrigger trigger,
            EvaluationContext context,
            Long userId) {
        
        String title = trigger.title().isEmpty() ? 
                trigger.type().getTitle() : 
                evaluateString(trigger.title(), context);
                
        String message = trigger.message().isEmpty() ? 
                trigger.type().getDefaultMessage() : 
                evaluateString(trigger.message(), context);
                
        String targetUrl = evaluateString(trigger.targetUrl(), context);
        
        Map<String, Object> data = new HashMap<>();
        data.put("triggeredBy", "annotation");
        data.put("type", trigger.type().name());
        
        return NotificationCreateRequest.builder()
                .userId(userId)
                .type(trigger.type())
                .customTitle(title)
                .customMessage(message)
                .targetUrl(targetUrl)
                .data(data)
                .build();
    }
    
    private EvaluationContext createEvaluationContext(JoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 메소드 파라미터 추가
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParameterNames(joinPoint);
        
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        
        // 결과 추가
        context.setVariable("result", result);
        
        return context;
    }
    
    private String[] getParameterNames(JoinPoint joinPoint) {
        // 실제 구현에서는 리플렉션이나 파라미터 이름 디스커버리 사용
        // 여기서는 간단한 예시
        return new String[]{"request", "student", "classRoom", "exam", "user"};
    }
    
    private boolean evaluateCondition(String condition, EvaluationContext context) {
        if (condition.isEmpty() || "true".equals(condition)) {
            return true;
        }
        
        try {
            Expression exp = parser.parseExpression(condition);
            Boolean result = exp.getValue(context, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to evaluate condition: " + condition, e);
            return false;
        }
    }
    
    private Long evaluateUserId(String expression, EvaluationContext context) {
        if (expression.isEmpty()) {
            return null;
        }
        
        try {
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to evaluate user ID expression: " + expression, e);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Long> evaluateUserIds(String expression, EvaluationContext context) {
        if (expression.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            
            if (value instanceof List) {
                return (List<Long>) value;
            } else if (value instanceof Collection) {
                return new ArrayList<>((Collection<Long>) value);
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to evaluate user IDs expression: " + expression, e);
            return Collections.emptyList();
        }
    }
    
    private String evaluateString(String expression, EvaluationContext context) {
        if (expression.isEmpty()) {
            return "";
        }
        
        try {
            Expression exp = parser.parseExpression(expression);
            return exp.getValue(context, String.class);
        } catch (Exception e) {
            log.error("Failed to evaluate string expression: " + expression, e);
            return expression;
        }
    }
}