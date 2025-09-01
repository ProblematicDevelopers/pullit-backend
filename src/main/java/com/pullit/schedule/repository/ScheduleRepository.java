package com.pullit.schedule.repository;

import com.pullit.schedule.entity.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    // 예정된 일정 조회
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.createdBy = :userId " +
           "AND s.scheduledDate >= :fromDate " +
           "AND s.status = 'upcoming' " +
           "ORDER BY s.scheduledDate ASC")
    List<Schedule> findUpcomingSchedules(@Param("userId") Long userId,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         Pageable pageable);
    
    // 특정 날짜 범위의 일정 조회
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.createdBy = :userId " +
           "AND s.scheduledDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.scheduledDate ASC")
    List<Schedule> findByDateRange(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // 오늘 일정 수 조회
    @Query("SELECT COUNT(s) FROM Schedule s " +
           "WHERE s.createdBy = :userId " +
           "AND DATE(s.scheduledDate) = :today " +
           "AND s.type = :type " +
           "AND s.status != 'cancelled'")
    Integer countTodaySchedules(@Param("userId") Long userId,
                                @Param("today") LocalDate today,
                                @Param("type") String type);
    
    // 학급별 일정 조회
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.classId = :classId " +
           "AND s.scheduledDate >= :fromDate " +
           "ORDER BY s.scheduledDate ASC")
    List<Schedule> findByClassId(@Param("classId") Long classId,
                                 @Param("fromDate") LocalDateTime fromDate);
    
    // 시험 일정 조회
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.examId = :examId")
    Schedule findByExamId(@Param("examId") Long examId);
    
    // 월별 일정 조회
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.createdBy = :userId " +
           "AND YEAR(s.scheduledDate) = :year " +
           "AND MONTH(s.scheduledDate) = :month " +
           "ORDER BY s.scheduledDate ASC")
    List<Schedule> findByMonth(@Param("userId") Long userId,
                               @Param("year") int year,
                               @Param("month") int month);
}