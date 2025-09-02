package com.pullit.domain.assignment.repository;

import com.pullit.domain.assignment.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    
    // 학생의 특정 과제 제출물 조회
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    
    // 과제별 모든 제출물 조회 (선생님용)
    Page<Submission> findByAssignmentId(Long assignmentId, Pageable pageable);
    
    // 과제별 제출 현황 통계
    @Query("SELECT s.status, COUNT(s) FROM Submission s " +
           "WHERE s.assignment.id = :assignmentId " +
           "GROUP BY s.status")
    List<Object[]> getSubmissionStatsByAssignmentId(@Param("assignmentId") Long assignmentId);
    
    // 학생의 모든 제출물 조회
    Page<Submission> findByStudentId(Long studentId, Pageable pageable);
    
    // 미제출 학생 목록 조회
    @Query("SELECT s FROM Submission s " +
           "WHERE s.assignment.id = :assignmentId " +
           "AND s.status = 'NOT_SUBMITTED'")
    List<Submission> findNotSubmittedByAssignmentId(@Param("assignmentId") Long assignmentId);
    
    // 제출물 상세 조회 (파일 포함)
    @Query("SELECT s FROM Submission s " +
           "LEFT JOIN FETCH s.files " +
           "WHERE s.id = :id")
    Optional<Submission> findByIdWithFiles(@Param("id") Long id);
    
    // 평가 대기중인 제출물 조회
    @Query("SELECT s FROM Submission s " +
           "WHERE s.assignment.teacherId = :teacherId " +
           "AND s.status = 'SUBMITTED' " +
           "ORDER BY s.submittedAt ASC")
    List<Submission> findPendingGradingByTeacherId(@Param("teacherId") Long teacherId);
}