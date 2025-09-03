package com.pullit.domain.calendar.repository;

import com.pullit.domain.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    
    // 사용자의 특정 기간 이벤트 조회
    @Query("SELECT e FROM CalendarEvent e " +
           "WHERE e.userId = :userId " +
           "AND ((e.startDateTime BETWEEN :startDate AND :endDate) " +
           "OR (e.endDateTime BETWEEN :startDate AND :endDate) " +
           "OR (e.startDateTime <= :startDate AND e.endDateTime >= :endDate)) " +
           "ORDER BY e.startDateTime")
    List<CalendarEvent> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    // 사용자의 특정 타입 이벤트 조회
    List<CalendarEvent> findByUserIdAndEventType(Long userId, CalendarEvent.EventType eventType);
    
    // 관련 ID로 이벤트 찾기 (과제, 시험 등과 연결)
    Optional<CalendarEvent> findByRelatedIdAndEventType(Long relatedId, CalendarEvent.EventType eventType);
    
    // 사용자의 오늘 일정 조회
    @Query("SELECT e FROM CalendarEvent e " +
           "WHERE e.userId = :userId " +
           "AND DATE(e.startDateTime) = DATE(:today) " +
           "ORDER BY e.startDateTime")
    List<CalendarEvent> findTodayEvents(@Param("userId") Long userId, @Param("today") LocalDateTime today);
    
    // 사용자의 예정된 이벤트 조회
    @Query("SELECT e FROM CalendarEvent e " +
           "WHERE e.userId = :userId " +
           "AND e.startDateTime > :now " +
           "AND e.status = 'SCHEDULED' " +
           "ORDER BY e.startDateTime")
    List<CalendarEvent> findUpcomingEvents(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    // 알림이 필요한 이벤트 조회
    @Query("SELECT e FROM CalendarEvent e " +
           "WHERE e.reminder = true " +
           "AND e.status = 'SCHEDULED' " +
           "AND e.startDateTime BETWEEN :start AND :end")
    List<CalendarEvent> findEventsNeedingReminder(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    
    // 학급 전체 일정 조회
    @Query("SELECT e FROM CalendarEvent e " +
           "WHERE e.classId = :classId " +
           "AND e.visibility = :visibility " +
           "AND ((e.startDateTime BETWEEN :startDate AND :endDate) " +
           "OR (e.endDateTime BETWEEN :startDate AND :endDate) " +
           "OR (e.startDateTime <= :startDate AND e.endDateTime >= :endDate)) " +
           "ORDER BY e.startDateTime")
    List<CalendarEvent> findByClassIdAndVisibilityAndDateRange(
            @Param("classId") Long classId,
            @Param("visibility") CalendarEvent.EventVisibility visibility,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}