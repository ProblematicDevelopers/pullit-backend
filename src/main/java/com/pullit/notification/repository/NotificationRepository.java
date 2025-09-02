package com.pullit.notification.repository;

import com.pullit.notification.entity.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 저장소 인터페이스
 * Redis를 주 저장소로 사용하며, 필요시 DB 백업 가능
 */
@Repository
public interface NotificationRepository {
    
    /**
     * 알림 저장
     */
    Notification save(Notification notification);
    
    /**
     * ID로 알림 조회
     */
    Optional<Notification> findById(String id);
    
    /**
     * 사용자 ID로 알림 목록 조회
     */
    List<Notification> findByUserId(Long userId);
    
    /**
     * 사용자의 읽지 않은 알림 조회
     */
    List<Notification> findUnreadByUserId(Long userId);
    
    /**
     * 사용자의 알림 목록 조회 (페이징, 최신순 정렬)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, int page, int size);
    
    /**
     * 알림 삭제
     */
    void deleteById(String id);
    
    /**
     * 사용자의 모든 알림 삭제
     */
    void deleteAllByUserId(Long userId);
}