package com.pullit.notification.service;

import com.pullit.notification.dto.request.NotificationCreateRequest;
import com.pullit.notification.dto.response.NotificationResponse;
import com.pullit.notification.entity.Notification;
import com.pullit.notification.enums.NotificationType;
import com.pullit.notification.repository.NotificationRepository;
import com.pullit.notification.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    
    private static final String NOTIFICATION_KEY_PREFIX = "notification:user:";
    private static final String UNREAD_COUNT_KEY_PREFIX = "notification:unread:";
    private static final String NOTIFICATION_CHANNEL = "notification-channel";
    private static final long NOTIFICATION_TTL_DAYS = 30;
    
    /**
     * 알림 생성 및 저장
     */
    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        try {
            // 알림 엔티티 생성
            Notification notification = buildNotification(request);
            
            // Redis에 저장
            saveToRedis(notification);
            
            // 읽지 않은 알림 카운트 증가
            incrementUnreadCount(notification.getUserId());
            
            // Redis Pub/Sub으로 실시간 전송
            publishNotification(notification);
            
            // WebSocket으로 즉시 전송
            sendViaWebSocket(notification);
            
            log.info("Notification created for user {}: {}", 
                    notification.getUserId(), notification.getTitle());
            
            return NotificationResponse.from(notification);
            
        } catch (Exception e) {
            log.error("Failed to create notification: ", e);
            throw new RuntimeException("알림 생성 실패", e);
        }
    }
    
    /**
     * 사용자의 알림 목록 조회
     */
    public List<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        
        // Redis List에서 페이징 처리하여 조회
        long start = (long) page * size;
        long end = start + size - 1;
        
        List<Object> notifications = redisTemplate.opsForList()
                .range(key, start, end);
        
        if (notifications == null || notifications.isEmpty()) {
            return Collections.emptyList();
        }
        
        return notifications.stream()
                .map(this::convertToNotification)
                .filter(Objects::nonNull)
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        return count != null ? count : 0;
    }
    
    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, String notificationId) {
        String listKey = NOTIFICATION_KEY_PREFIX + userId;
        
        // Redis List에서 모든 알림 조회
        List<Object> notifications = redisTemplate.opsForList()
                .range(listKey, 0, -1);
        
        if (notifications == null) {
            return;
        }
        
        // 해당 알림 찾아서 읽음 처리
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = convertToNotification(notifications.get(i));
            if (notification != null && notification.getId().equals(notificationId)) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                
                // 업데이트된 알림으로 교체
                redisTemplate.opsForList().set(listKey, i, notification);
                
                // 읽지 않은 알림 카운트 감소
                decrementUnreadCount(userId);
                
                log.info("Notification {} marked as read for user {}", 
                        notificationId, userId);
                break;
            }
        }
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        String listKey = NOTIFICATION_KEY_PREFIX + userId;
        List<Object> notifications = redisTemplate.opsForList()
                .range(listKey, 0, -1);
        
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        
        // 모든 알림을 읽음 처리
        List<Notification> updatedNotifications = new ArrayList<>();
        for (Object obj : notifications) {
            Notification notification = convertToNotification(obj);
            if (notification != null && !notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
            }
            updatedNotifications.add(notification);
        }
        
        // Redis 리스트 전체 교체
        redisTemplate.delete(listKey);
        for (Notification notification : updatedNotifications) {
            redisTemplate.opsForList().rightPush(listKey, notification);
        }
        
        // 읽지 않은 알림 카운트 리셋
        resetUnreadCount(userId);
        
        log.info("All notifications marked as read for user {}", userId);
    }
    
    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long userId, String notificationId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        List<Object> notifications = redisTemplate.opsForList()
                .range(key, 0, -1);
        
        if (notifications == null) {
            return;
        }
        
        for (Object obj : notifications) {
            Notification notification = convertToNotification(obj);
            if (notification != null && notification.getId().equals(notificationId)) {
                redisTemplate.opsForList().remove(key, 1, obj);
                
                if (!notification.isRead()) {
                    decrementUnreadCount(userId);
                }
                
                log.info("Notification {} deleted for user {}", 
                        notificationId, userId);
                break;
            }
        }
    }
    
    /**
     * 오래된 알림 정리 (배치 작업용)
     */
    public void cleanupOldNotifications() {
        // 모든 사용자 키 조회
        Set<String> keys = redisTemplate.keys(NOTIFICATION_KEY_PREFIX + "*");
        if (keys == null) {
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(NOTIFICATION_TTL_DAYS);
        
        for (String key : keys) {
            List<Object> notifications = redisTemplate.opsForList()
                    .range(key, 0, -1);
            
            if (notifications == null) {
                continue;
            }
            
            List<Notification> validNotifications = new ArrayList<>();
            for (Object obj : notifications) {
                Notification notification = convertToNotification(obj);
                if (notification != null && 
                    notification.getCreatedAt().isAfter(cutoffDate)) {
                    validNotifications.add(notification);
                }
            }
            
            // 유효한 알림만 다시 저장
            if (validNotifications.size() < notifications.size()) {
                redisTemplate.delete(key);
                for (Notification notification : validNotifications) {
                    redisTemplate.opsForList().rightPush(key, notification);
                }
            }
        }
        
        log.info("Old notifications cleanup completed");
    }
    
    // Private helper methods
    
    private Notification buildNotification(NotificationCreateRequest request) {
        String id = UUID.randomUUID().toString();
        
        return Notification.builder()
                .id(id)
                .userId(request.getUserId())
                .type(request.getType().name())
                .title(request.getCustomTitle() != null ? 
                        request.getCustomTitle() : request.getType().getTitle())
                .message(request.getCustomMessage() != null ? 
                        request.getCustomMessage() : request.getType().getDefaultMessage())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .data(request.getData())
                .targetUrl(request.getTargetUrl())
                .priority(request.getType().getPriority())
                .build();
    }
    
    private void saveToRedis(Notification notification) {
        String key = NOTIFICATION_KEY_PREFIX + notification.getUserId();
        
        // 리스트 앞쪽에 추가 (최신 알림이 먼저 오도록)
        redisTemplate.opsForList().leftPush(key, notification);
        
        // TTL 설정
        redisTemplate.expire(key, NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);
        
        // 리스트 크기 제한 (최대 100개)
        redisTemplate.opsForList().trim(key, 0, 99);
    }
    
    private void incrementUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);
    }
    
    private void decrementUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(key, 0);
        }
    }
    
    private void resetUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, 0);
    }
    
    private void publishNotification(Notification notification) {
        try {
            String message = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, message);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis channel: ", e);
        }
    }
    
    private void sendViaWebSocket(Notification notification) {
        try {
            webSocketHandler.sendNotificationToUser(
                    notification.getUserId(), 
                    NotificationResponse.from(notification)
            );
        } catch (Exception e) {
            log.error("Failed to send notification via WebSocket: ", e);
        }
    }
    
    private Notification convertToNotification(Object obj) {
        try {
            if (obj instanceof Notification) {
                return (Notification) obj;
            }
            return objectMapper.convertValue(obj, Notification.class);
        } catch (Exception e) {
            log.error("Failed to convert object to Notification: ", e);
            return null;
        }
    }
}