package com.pullit.exam.repository;

import com.pullit.exam.entity.ExamAssignment;
import com.pullit.exam.entity.ExamAssignment.ExamAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시험 출제 정보 Repository
 */
@Repository
public interface ExamAssignmentRepository extends JpaRepository<ExamAssignment, Long> {

    /**
     * 특정 시험과 학급에 대한 출제 정보 조회
     */
    Optional<ExamAssignment> findByUserExamIdAndClassEntityClassId(Long userExamId, Long classId);

    /**
     * 특정 시험의 모든 출제 정보 조회
     */
    List<ExamAssignment> findByUserExamId(Long userExamId);

    /**
     * 특정 학급의 모든 출제 정보 조회
     */
    List<ExamAssignment> findByClassEntityClassId(Long classId);

    /**
     * 특정 날짜의 시험 출제 정보 조회
     */
    List<ExamAssignment> findByExamDate(LocalDate examDate);

    /**
     * 특정 학급의 특정 날짜 시험 출제 정보 조회
     */
    List<ExamAssignment> findByClassEntityClassIdAndExamDate(Long classId, LocalDate examDate);

    /**
     * 특정 상태의 시험 출제 정보 조회
     */
    List<ExamAssignment> findByStatus(ExamAssignmentStatus status);

    /**
     * 알림이 발송되지 않은 예정된 시험 조회
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.status = :status " +
           "AND ea.notificationSent = false " +
           "AND ea.examStartDateTime > :now " +
           "ORDER BY ea.examStartDateTime ASC")
    List<ExamAssignment> findUpcomingExamsWithoutNotification(
            @Param("status") ExamAssignmentStatus status,
            @Param("now") LocalDateTime now);

    /**
     * 특정 시간 범위 내의 시험 조회 (알림 발송용)
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.status = 'SCHEDULED' " +
           "AND ea.notificationSent = false " +
           "AND ea.examStartDateTime BETWEEN :startTime AND :endTime")
    List<ExamAssignment> findExamsInTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 특정 학급의 활성 시험 조회 (현재 진행 중인 시험)
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.classEntity.classId = :classId " +
           "AND ea.status = 'SCHEDULED' " +
           "AND :now BETWEEN ea.examStartDateTime AND ea.examEndDateTime")
    List<ExamAssignment> findActiveExamsByClassId(
            @Param("classId") Long classId,
            @Param("now") LocalDateTime now);

    /**
     * 특정 학급의 예정된 시험 조회
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.classEntity.classId = :classId " +
           "AND ea.status = 'SCHEDULED' " +
           "AND ea.examStartDateTime > :now " +
           "ORDER BY ea.examStartDateTime ASC")
    List<ExamAssignment> findUpcomingExamsByClassId(
            @Param("classId") Long classId,
            @Param("now") LocalDateTime now);

    /**
     * 종료 시간이 지났지만 상태가 업데이트되지 않은 시험 조회
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.status = 'SCHEDULED' " +
           "AND ea.examEndDateTime < :now")
    List<ExamAssignment> findExpiredExams(@Param("now") LocalDateTime now);

    /**
     * 중복 출제 확인 - 같은 시험이 같은 학급에 이미 예정되어 있는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END " +
           "FROM ExamAssignment ea " +
           "WHERE ea.userExam.id = :userExamId " +
           "AND ea.classEntity.classId = :classId " +
           "AND ea.status IN ('SCHEDULED', 'IN_PROGRESS')")
    boolean existsActiveAssignment(@Param("userExamId") Long userExamId, @Param("classId") Long classId);

    /**
     * 특정 기간 내의 시험 출제 정보 조회
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "WHERE ea.examDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ea.examDate ASC, ea.examTime ASC")
    List<ExamAssignment> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 학생이 속한 학급의 시험 출제 정보 조회
     */
    @Query("SELECT ea FROM ExamAssignment ea " +
           "JOIN com.pullit.student.entity.Student s ON s.classGroupID = ea.classEntity.classId " +
           "WHERE s.userId = :studentId " +
           "AND ea.status = :status " +
           "ORDER BY ea.examStartDateTime ASC")
    List<ExamAssignment> findByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") ExamAssignmentStatus status);
}