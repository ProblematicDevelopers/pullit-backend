package com.pullit.notification.repository;

import com.pullit.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {
    
    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, int page, int size) {
        // Redis에서는 이미 findByUserId로 구현되어 있으므로 그것을 사용
        // 페이징은 간단하게 skip과 limit로 처리
        List<Notification> allNotifications = findByUserId(userId);
        int start = page * size;
        int end = Math.min(start + size, allNotifications.size());
        
        if (start >= allNotifications.size()) {
            return new ArrayList<>();
        }
        
        return allNotifications.subList(start, end);
    }
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "notification:";
    
    @Override
    public Notification save(Notification notification) {
        String key = KEY_PREFIX + notification.getId();
        redisTemplate.opsForValue().set(key, notification);
        return notification;
    }
    
    @Override
    public Optional<Notification> findById(String id) {
        String key = KEY_PREFIX + id;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof Notification) {
            return Optional.of((Notification) obj);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Notification> findByUserId(Long userId) {
        String pattern = KEY_PREFIX + "user:" + userId + ":*";
        return redisTemplate.keys(pattern).stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(obj -> obj instanceof Notification)
                .map(obj -> (Notification) obj)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        return findByUserId(userId).stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(String id) {
        String key = KEY_PREFIX + id;
        redisTemplate.delete(key);
    }
    
    @Override
    public void deleteAllByUserId(Long userId) {
        String pattern = KEY_PREFIX + "user:" + userId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}