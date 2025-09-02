package com.pullit.notification.repository;

import com.pullit.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 JPA 저장소
 * 데이터베이스에 영구 저장하기 위한 JPA Repository
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<Notification, String> {
    
    /**
     * 사용자 ID로 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 사용자 ID로 알림 목록 조회 (페이징)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 모든 알림 삭제
     */
    void deleteAllByUserId(Long userId);
    
    /**
     * 특정 날짜 이전의 알림 삭제 (배치 작업용)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * 특정 타입의 알림 조회
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
}